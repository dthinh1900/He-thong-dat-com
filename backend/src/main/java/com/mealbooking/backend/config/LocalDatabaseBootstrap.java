package com.mealbooking.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;

/**
 * Runs before the application context creates Hikari, Flyway or JPA.
 * Only provisions the dedicated local development database when it is absent.
 */
public final class LocalDatabaseBootstrap
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(LocalDatabaseBootstrap.class);
    private static final String DATABASE_MISSING = "3D000";
    private static final String DATABASE_ALREADY_EXISTS = "42P04";
    private static final Pattern LOCAL_URL = Pattern.compile(
            "^(jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1|\\[::1\\])(?::[0-9]{1,5})?)/meal_booking_db$",
            Pattern.CASE_INSENSITIVE);
    private static final String SCRIPT = "db/bootstrap/V0__create_database.sql";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        bootstrap(context.getEnvironment(), DriverManager::getConnection);
    }

    static void bootstrap(Environment environment, ConnectionFactory connections) {
        if (!environment.acceptsProfiles(Profiles.of("local"))
                || !environment.getProperty("app.database.bootstrap.enabled", Boolean.class, true)) {
            return;
        }

        String targetUrl = environment.getProperty("spring.datasource.url", "");
        String maintenanceUrl = maintenanceUrl(targetUrl);
        if (maintenanceUrl == null) {
            // Never provision a remote server, a different database or a custom URL.
            return;
        }

        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password", "");

        try (Connection ignored = connections.open(targetUrl, username, password)) {
            log.info("V0 bootstrap: meal_booking_db already exists; skipping creation.");
            return;
        } catch (SQLException e) {
            // Only a missing database permits creation; bad passwords, network
            // errors and permission failures must not trigger a CREATE DATABASE.
            if (!DATABASE_MISSING.equals(e.getSQLState())) {
                throw new IllegalStateException("V0 bootstrap: cannot connect to the local meal_booking_db.", e);
            }
        }

        log.info("V0 bootstrap: creating missing local meal_booking_db.");
        try (Connection connection = connections.open(maintenanceUrl, username, password)) {
            // CREATE DATABASE is a top-level PostgreSQL command, not a migration transaction.
            connection.setAutoCommit(true);
            String sql = new ClassPathResource(SCRIPT).getContentAsString(StandardCharsets.UTF_8);
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                if (!DATABASE_ALREADY_EXISTS.equals(e.getSQLState())) {
                    throw e;
                }
                // Another local instance created the database after our first check.
                log.info("V0 bootstrap: meal_booking_db was created concurrently; continuing.");
            }
            log.info("V0 bootstrap: database ready for Flyway V1 and later.");
        } catch (SQLException | IOException e) {
            throw new IllegalStateException(
                    "V0 bootstrap failed. Check local PostgreSQL, CREATEDB permission and read-write access.", e);
        }
    }

    static String maintenanceUrl(String targetUrl) {
        var matcher = LOCAL_URL.matcher(targetUrl);
        return matcher.matches() ? matcher.group(1) + "/postgres" : null;
    }

    @FunctionalInterface
    interface ConnectionFactory {
        Connection open(String url, String username, String password) throws SQLException;
    }
}

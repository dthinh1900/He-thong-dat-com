package com.mealbooking.backend.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class LocalDatabaseBootstrapTest {

    private static final String TARGET = "jdbc:postgresql://localhost:5432/meal_booking_db";
    private static final String POSTGRES = "jdbc:postgresql://localhost:5432/postgres";

    private MockEnvironment local() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", TARGET)
                .withProperty("spring.datasource.username", "postgres")
                .withProperty("spring.datasource.password", "test-only");
        env.setDefaultProfiles("local");
        return env;
    }

    @Test
    void existingDatabaseIsNotModified() throws SQLException {
        var factory = mock(LocalDatabaseBootstrap.ConnectionFactory.class);
        var connection = mock(Connection.class);
        when(factory.open(TARGET, "postgres", "test-only")).thenReturn(connection);

        LocalDatabaseBootstrap.bootstrap(local(), factory);

        verify(connection).close();
        verify(factory, never()).open(POSTGRES, "postgres", "test-only");
    }

    @Test
    void missingDatabaseIsCreatedOutsideTransaction() throws SQLException {
        var factory = mock(LocalDatabaseBootstrap.ConnectionFactory.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(factory.open(TARGET, "postgres", "test-only"))
                .thenThrow(new SQLException("missing", "3D000"));
        when(factory.open(POSTGRES, "postgres", "test-only")).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        LocalDatabaseBootstrap.bootstrap(local(), factory);

        var order = inOrder(connection, statement);
        order.verify(connection).setAutoCommit(true);
        order.verify(connection).createStatement();
        order.verify(statement).execute(contains("CREATE DATABASE meal_booking_db;"));
        verify(connection).close();
    }

    @Test
    void authenticationFailureDoesNotCreateDatabase() throws SQLException {
        var factory = mock(LocalDatabaseBootstrap.ConnectionFactory.class);
        when(factory.open(TARGET, "postgres", "test-only"))
                .thenThrow(new SQLException("authentication failed", "28P01"));

        assertThrows(IllegalStateException.class, () -> LocalDatabaseBootstrap.bootstrap(local(), factory));
        verify(factory, never()).open(POSTGRES, "postgres", "test-only");
    }

    @Test
    void remoteDatabaseIsNeverProvisioned() {
        var factory = mock(LocalDatabaseBootstrap.ConnectionFactory.class);
        var env = local().withProperty("spring.datasource.url", "jdbc:postgresql://remote.example:5432/meal_booking_db");

        LocalDatabaseBootstrap.bootstrap(env, factory);

        verifyNoInteractions(factory);
    }

    @Test
    void productionProfileAndDisabledBootstrapAreSkipped() {
        var factory = mock(LocalDatabaseBootstrap.ConnectionFactory.class);
        var prod = local();
        prod.setActiveProfiles("prod");
        LocalDatabaseBootstrap.bootstrap(prod, factory);
        LocalDatabaseBootstrap.bootstrap(local().withProperty("app.database.bootstrap.enabled", "false"), factory);

        verifyNoInteractions(factory);
    }

    @Test
    void onlyDedicatedPlainLoopbackUrlsAreAccepted() {
        assertEquals(POSTGRES, LocalDatabaseBootstrap.maintenanceUrl(TARGET));
        assertEquals("jdbc:postgresql://127.0.0.1:5433/postgres",
                LocalDatabaseBootstrap.maintenanceUrl("jdbc:postgresql://127.0.0.1:5433/meal_booking_db"));
        assertNull(LocalDatabaseBootstrap.maintenanceUrl("jdbc:postgresql://localhost:5432/postgres"));
        assertNull(LocalDatabaseBootstrap.maintenanceUrl(TARGET + "?targetServerType=secondary"));
    }
}

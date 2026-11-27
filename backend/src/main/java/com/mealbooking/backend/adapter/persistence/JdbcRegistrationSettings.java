package com.mealbooking.backend.adapter.persistence;

import com.mealbooking.backend.application.port.out.RegistrationSettingsStore;
import com.mealbooking.backend.application.model.AdminResponses.Settings;
import java.time.LocalTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRegistrationSettings implements RegistrationSettingsStore {
    private final JdbcTemplate jdbc;

    public JdbcRegistrationSettings(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Settings read() {
        return jdbc.queryForObject("select cutoff, cancel_deadline from registration_settings where id=1",
            (rs,n)->new Settings(rs.getObject(1,LocalTime.class),rs.getObject(2,LocalTime.class)));
    }
    public void save(Settings settings) {
        jdbc.update("update registration_settings set cutoff=?, cancel_deadline=? where id=1",
            settings.cutoff(), settings.cancelDeadline());
    }
}

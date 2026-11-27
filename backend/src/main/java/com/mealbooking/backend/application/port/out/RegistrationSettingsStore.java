package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.model.AdminResponses.Settings;

public interface RegistrationSettingsStore {
    Settings read();

    void save(Settings settings);
}

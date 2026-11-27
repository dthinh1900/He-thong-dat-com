package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.AdminResponses.Settings;

public interface RegistrationSettings {
    Settings settings();

    void update(Settings settings);
}

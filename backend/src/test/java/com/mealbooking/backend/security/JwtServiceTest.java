package com.mealbooking.backend.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.systemUTC();

    @Test
    void generatesEphemeralSecretWhenLocalSecretIsEmpty() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertDoesNotThrow(() -> new JwtService("", 480, clock, objectMapper, environment));
    }

    @Test
    void stillRequiresSecretOutsideLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class,
                () -> new JwtService("", 480, clock, objectMapper, environment));
    }

    @Test
    void rejectsExplicitShortSecretEvenForLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThrows(IllegalStateException.class,
                () -> new JwtService("too-short", 480, clock, objectMapper, environment));
    }
}

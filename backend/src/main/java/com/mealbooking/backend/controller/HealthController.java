
package com.mealbooking.backend.controller;

import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final Environment environment;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        String[] profiles = environment.getActiveProfiles();
        String mode = profiles.length == 0
                ? "default"
                : String.join(",", profiles);

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "meal-booking-api",
                "profile", mode));
    }
}

package com.mealbooking.backend.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class MealRegistration {
    private Long id;
    private String registrationCode;
    private User user;
    private Integer month;
    private Integer year;
    private LocalDateTime createdAt;
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}

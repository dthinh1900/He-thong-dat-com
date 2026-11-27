package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.Role;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String employeeCode,
    String username,
    String fullName,
    String email,
    String phone,
    String departmentName,
    Role role,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

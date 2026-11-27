package com.mealbooking.backend.domain.model;

import com.mealbooking.backend.domain.enums.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class User {
    private Long id;
    private String employeeCode;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private Department department;
    private Role role = Role.USER;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}

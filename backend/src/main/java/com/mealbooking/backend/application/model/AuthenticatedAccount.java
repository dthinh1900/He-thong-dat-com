package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.Role;

public record AuthenticatedAccount(Long id, String username, Role role) {}

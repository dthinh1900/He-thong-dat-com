package com.mealbooking.backend.application.model;

public record LoginResponse(String token, String tokenType, UserResponse user) { }

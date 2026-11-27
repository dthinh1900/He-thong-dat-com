package com.mealbooking.backend.application.port.out;

public interface PasswordHasher {
    String encode(String password);
}

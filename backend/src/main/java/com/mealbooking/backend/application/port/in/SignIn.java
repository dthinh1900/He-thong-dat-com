package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.LoginResponse;

public interface SignIn {
    LoginResponse login(String username, String password);
}

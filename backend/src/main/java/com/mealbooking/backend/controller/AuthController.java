package com.mealbooking.backend.controller;

import com.mealbooking.backend.dto.request.LoginRequest;
import com.mealbooking.backend.application.model.LoginResponse;
import com.mealbooking.backend.application.port.in.SignIn;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SignIn authService;
    public AuthController(SignIn authService) { this.authService = authService; }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }
}

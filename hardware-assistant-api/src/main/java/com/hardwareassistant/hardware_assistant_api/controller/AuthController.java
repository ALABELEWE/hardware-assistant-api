package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.request.LoginRequest;
import com.hardwareassistant.hardware_assistant_api.dto.request.RegisterRequest;
import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.AuthResponse;
import com.hardwareassistant.hardware_assistant_api.service.AuthService;
import com.hardwareassistant.hardware_assistant_api.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token is required"));
        }
        try {
            emailVerificationService.verifyEmail(token);
            return ResponseEntity.ok(
                    ApiResponse.success("Email verified successfully!", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email is required"));
        }
        try {
            emailVerificationService.sendVerificationEmail(email);
            return ResponseEntity.ok(
                    ApiResponse.success("Verification email sent!", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.request.LoginRequest;
import com.hardwareassistant.hardware_assistant_api.dto.request.RegisterRequest;
import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.AuthResponse;
import com.hardwareassistant.hardware_assistant_api.service.AuthService;
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

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(
                    ApiResponse.success("Email verified successfully. You can now log in.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Verification link has expired or already been used. Please request a new one."));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(
            @RequestBody Map<String, String> body) {
        try {
            authService.resendVerificationEmail(body.get("email"));
            return ResponseEntity.ok(
                    ApiResponse.success("Verification email sent. Please check your inbox.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Could not send verification email: " + e.getMessage()));
        }
    }
}
package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.dto.request.LoginRequest;
import com.hardwareassistant.hardware_assistant_api.dto.request.RegisterRequest;
import com.hardwareassistant.hardware_assistant_api.dto.response.AuthResponse;
import com.hardwareassistant.hardware_assistant_api.exception.BusinessException;
import com.hardwareassistant.hardware_assistant_api.exception.EmailNotVerifiedException;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.UserRepository;
import com.hardwareassistant.hardware_assistant_api.service.AuthService;
import com.hardwareassistant.hardware_assistant_api.service.EmailVerificationService;
import com.hardwareassistant.hardware_assistant_api.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository           userRepository;
    private final JwtUtil                  jwtUtil;
    private final AuthenticationManager    authenticationManager;
    private final EmailVerificationService emailVerificationService;
    private final UserRegistrationHelper registrationHelper;

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Runs in its own transaction via separate bean — fully commits before email is sent
        User savedUser = registrationHelper.createAndSave(
                request.getEmail(), request.getPassword());

        // Email sent AFTER transaction commits — failure cannot roll back registration
        try {
            emailVerificationService.sendVerificationEmail(savedUser.getEmail());
        } catch (Exception e) {
            log.warn("Could not send verification email to {}: {}",
                    savedUser.getEmail(), e.getMessage());
        }

        return AuthResponse.builder()
                .message("Registration successful. Please check your email to verify your account.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (user.getRole() == User.Role.MERCHANT && !user.isEmailVerified()) {
            throw new EmailNotVerifiedException(
                    "Please verify your email before logging in. Check your inbox.");
        }

        String token = jwtUtil.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
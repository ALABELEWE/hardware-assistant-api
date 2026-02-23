package com.hardwareassistant.hardware_assistant_api.service;

import com.hardwareassistant.hardware_assistant_api.dto.request.LoginRequest;
import com.hardwareassistant.hardware_assistant_api.dto.request.RegisterRequest;
import com.hardwareassistant.hardware_assistant_api.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}

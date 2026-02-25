package com.hardwareassistant.hardware_assistant_api.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
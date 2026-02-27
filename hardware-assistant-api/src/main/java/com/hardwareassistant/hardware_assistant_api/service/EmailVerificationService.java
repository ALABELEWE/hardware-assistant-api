package com.hardwareassistant.hardware_assistant_api.service;

public interface EmailVerificationService {
    void sendVerificationEmail(String email);  // called on registration
    void verifyEmail(String token);            // called when user clicks link
    void resendVerification(String email);     // called when user requests resend
}
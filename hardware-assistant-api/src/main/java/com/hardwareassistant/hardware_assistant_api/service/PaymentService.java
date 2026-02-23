package com.hardwareassistant.hardware_assistant_api.service;

public interface PaymentService {
    void handleSuccessfulPayment(String paymentReference, String customerEmail, String planName);
    void handleCancelledPayment(String paymentProviderId);
    boolean verifyWebhookSignature(String payload, String signature);
}

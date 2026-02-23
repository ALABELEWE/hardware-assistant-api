package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.model.Subscription;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.SubscriptionRepository;
import com.hardwareassistant.hardware_assistant_api.repository.UserRepository;
import com.hardwareassistant.hardware_assistant_api.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Value("${app.payment.paystack-secret-key}")
    private String paystackSecretKey;

    @Value("${app.payment.paystack-webhook-secret}")
    private String webhookSecret;

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void handleSuccessfulPayment(String paymentReference, String customerEmail, String planName) {
        User user = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + customerEmail));

        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElse(Subscription.builder().user(user).build());

        subscription.setPlanName(planName);
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setPaymentProviderId(paymentReference);
        subscription.setRenewalDate(LocalDateTime.now().plusMonths(1));

        subscriptionRepository.save(subscription);
        log.info("Subscription activated for user: {}", customerEmail);
    }

    @Override
    @Transactional
    public void handleCancelledPayment(String paymentProviderId) {
        subscriptionRepository.findByPaymentProviderId(paymentProviderId).ifPresent(sub -> {
            sub.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(sub);
        });
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            mac.init(new javax.crypto.spec.SecretKeySpec(webhookSecret.getBytes(), "HmacSHA512"));
            byte[] hash = mac.doFinal(payload.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) hexString.append(String.format("%02x", b));
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }
}

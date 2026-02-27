package com.hardwareassistant.hardware_assistant_api.service.impl;


import com.africastalking.AfricasTalking;
import com.hardwareassistant.hardware_assistant_api.model.SmsLog;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.SmsLogRepository;
import com.hardwareassistant.hardware_assistant_api.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final SmsLogRepository smsLogRepository;
    private com.africastalking.SmsService smsService;

    @Value("${AT_API_KEY}")
    private String apiKey;

    @Value("${AT_USERNAME}")
    private String username;

    @Override
    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendSms(User merchant, String message,
                                UUID analysisId) {
        String phone = null;
                if(merchant.getMerchantProfile() != null){
                    phone = merchant.getMerchantProfile().getPhoneNumber();
                }

        if (phone == null || phone.isBlank()) {
            log.warn("No phone number for merchant: {}", merchant.getEmail());
            return;
        }

        String summerized = summarize(message);

        SmsLog smsLog = SmsLog.builder()
                .merchant(merchant)
                .phoneNumber(phone)
                .message(summarize(message))
                .status(SmsLog.Status.PENDING)
                .analysisId(analysisId)
                .attempts(0)
                .build();
        smsLogRepository.save(smsLog);

        try {
            AfricasTalking.initialize(username, apiKey);
            smsService = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);

            smsLog.setAttempts(smsLog.getAttempts() + 1);
            smsService.send(summarize(message), new String[]{phone}, false);


            smsLog.setStatus(SmsLog.Status.SENT);
            smsLog.setSentAt(LocalDateTime.now());
            log.info("SMS sent to: {}", phone);

        } catch (Exception e) {
            smsLog.setStatus(SmsLog.Status.FAILED);
            smsLog.setErrorMessage(e.getMessage());
            log.error("SMS failed for {}: {}", phone, e.getMessage());
        } finally {
            smsLogRepository.save(smsLog);
        }
    }

    private String summarize(String message) {
        if (message == null) return "";
        return message.length() > 160
                ? message.substring(0, 157) + "..."
                : message;
    }

}
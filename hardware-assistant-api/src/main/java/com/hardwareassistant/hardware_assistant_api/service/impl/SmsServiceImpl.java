package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.SmsLog;
import com.hardwareassistant.hardware_assistant_api.repository.SmsLogRepository;
import com.hardwareassistant.hardware_assistant_api.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Value("${app.sms.africastalking-api-key}")
    private String apiKey;

    @Value("${app.sms.africastalking-username}")
    private String username;

    private final SmsLogRepository smsLogRepository;
    private final WebClient.Builder webClientBuilder;


    @Override
    public void sendSms(MerchantProfile profile, String message) {
        SmsLog smsLog = SmsLog.builder()
                .merchantProfile(profile)
                .phoneNumber(profile.getPhoneNumber())
                .message(message)
                .status(SmsLog.SmsStatus.PENDING)
                .build();

        smsLog = smsLogRepository.save(smsLog);

        try {
            WebClient client = webClientBuilder
                    .baseUrl("https://api.africastalking.com")
                    .defaultHeader("apiKey", apiKey)
                    .defaultHeader("Accept", "application/json")
                    .build();

            client.post()
                    .uri("/version1/messaging")
                    .body(BodyInserters.fromFormData("username", username)
                            .with("to", profile.getPhoneNumber())
                            .with("message", message))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            smsLog.setStatus(SmsLog.SmsStatus.SENT);
            log.info("SMS sent to {}", profile.getPhoneNumber());

        } catch (Exception e) {
            smsLog.setStatus(SmsLog.SmsStatus.FAILED);
            log.error("SMS failed for {}: {}", profile.getPhoneNumber(), e.getMessage());
        }

        smsLogRepository.save(smsLog);
    }
}
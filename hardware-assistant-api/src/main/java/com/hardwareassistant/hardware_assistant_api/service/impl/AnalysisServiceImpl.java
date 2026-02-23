package com.hardwareassistant.hardware_assistant_api.service.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareassistant.hardware_assistant_api.exception.BusinessException;
import com.hardwareassistant.hardware_assistant_api.exception.ResourceNotFoundException;
import com.hardwareassistant.hardware_assistant_api.model.BusinessAnalysis;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.BusinessAnalysisRepository;
import com.hardwareassistant.hardware_assistant_api.repository.MerchantProfileRepository;
import com.hardwareassistant.hardware_assistant_api.service.AiService;
import com.hardwareassistant.hardware_assistant_api.service.AnalysisService;
import com.hardwareassistant.hardware_assistant_api.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

    private final AiService aiService;
    private final SmsService smsService;
    private final BusinessAnalysisRepository analysisRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ObjectMapper objectMapper;


    public Page<BusinessAnalysis> getAnalysisHistory(User user, Pageable pageable) {
        MerchantProfile profile = merchantProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant profile not found"));
        return analysisRepository.findByMerchantProfileOrderByCreatedAtDesc(profile, pageable);
    }

    @Override
    @Transactional
    public BusinessAnalysis generateAnalysis(User user, boolean sendSms) {
        MerchantProfile profile = merchantProfileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Please complete your merchant profile first"));

        String aiJson = aiService.generateBusinessInsights(profile);

        BusinessAnalysis analysis = BusinessAnalysis.builder()
                .merchantProfile(profile)
                .aiResponseJson(aiJson)
                .build();

        analysis = analysisRepository.save(analysis);

        // Send SMS if requested and phone number exists
        if (sendSms && profile.getPhoneNumber() != null) {
            try {
                JsonNode node = objectMapper.readTree(aiJson);
                String smsAlert = node.has("smsAlert") ? node.get("smsAlert").asText() : "New business insight available!";
                smsService.sendSms(profile, smsAlert);
            } catch (Exception e) {
                log.warn("Could not extract SMS alert from AI response: {}", e.getMessage());
            }
        }

        return analysis;
    }
}

package com.hardwareassistant.hardware_assistant_api.service;

import com.hardwareassistant.hardware_assistant_api.model.AiUsage;
import com.hardwareassistant.hardware_assistant_api.model.User;

import java.util.UUID;

public interface AiUsageService {
    void recordUsage(User merchant, int promptTokens, int completionTokens,
                     String model, UUID analysisId);
    void checkQuota(User merchant);
    Object getUsageSummary(UUID merchantId);
}
package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.model.AiUsage;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.AiUsageRepository;
import com.hardwareassistant.hardware_assistant_api.service.AiUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiUsageServiceImpl implements AiUsageService {

    private final AiUsageRepository aiUsageRepository;

    // Groq Llama 3.3 70B pricing per 1M tokens
    @Value("${app.ai.input-cost-per-million:0.59}")
    private double inputCostPerMillion;

    @Value("${app.ai.output-cost-per-million:0.79}")
    private double outputCostPerMillion;

    @Value("${app.ai.monthly-analysis-limit:50}")
    private int monthlyAnalysisLimit;

    @Override
    @Transactional
    public void recordUsage(User merchant, int promptTokens, int completionTokens,
                            String model, UUID analysisId) {
        int totalTokens = promptTokens + completionTokens;

        BigDecimal inputCost = BigDecimal.valueOf(
            (promptTokens / 1_000_000.0) * inputCostPerMillion);
        BigDecimal outputCost = BigDecimal.valueOf(
            (completionTokens / 1_000_000.0) * outputCostPerMillion);
        BigDecimal totalCost = inputCost.add(outputCost);

        AiUsage usage = AiUsage.builder()
            .merchant(merchant)
            .promptTokens(promptTokens)
            .completionTokens(completionTokens)
            .totalTokens(totalTokens)
            .estimatedCost(totalCost)
            .modelUsed(model)
            .analysisId(analysisId)
            .build();

        aiUsageRepository.save(usage);
        log.info("AI usage recorded: {} tokens, ${} for merchant: {}",
            totalTokens, totalCost, merchant.getEmail());
    }

    @Override
    public void checkQuota(User merchant) {
        YearMonth current = YearMonth.now();
        LocalDateTime from = current.atDay(1).atStartOfDay();
        LocalDateTime to   = current.atEndOfMonth().atTime(23, 59, 59);

        long monthlyCount = aiUsageRepository
            .countByMerchantIdAndPeriod(merchant.getId(), from, to);

        if (monthlyCount >= monthlyAnalysisLimit) {
            throw new RuntimeException(
                "Monthly analysis limit of " + monthlyAnalysisLimit +
                " reached. Upgrade your plan for more analyses.");
        }
    }

    @Override
    public Object getUsageSummary(UUID merchantId) {
        YearMonth current = YearMonth.now();
        LocalDateTime from = current.atDay(1).atStartOfDay();
        LocalDateTime to   = current.atEndOfMonth().atTime(23, 59, 59);

        long monthlyCount  = aiUsageRepository
            .countByMerchantIdAndPeriod(merchantId, from, to);
        Long monthlyTokens = aiUsageRepository
            .sumTokensByMerchantIdAndPeriod(merchantId, from, to);

        Map<String, Object> summary = new HashMap<>();
        summary.put("monthlyAnalysesUsed", monthlyCount);
        summary.put("monthlyLimit",        monthlyAnalysisLimit);
        summary.put("monthlyTokensUsed",   monthlyTokens != null ? monthlyTokens : 0);
        summary.put("remainingAnalyses",   Math.max(0, monthlyAnalysisLimit - monthlyCount));
        return summary;
    }
}
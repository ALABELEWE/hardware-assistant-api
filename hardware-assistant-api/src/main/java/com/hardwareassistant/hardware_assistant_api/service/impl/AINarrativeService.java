package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareassistant.hardware_assistant_api.dto.response.AINarrativeResult;
import com.hardwareassistant.hardware_assistant_api.dto.response.AINarrativeResult.ActionItem;
import com.hardwareassistant.hardware_assistant_api.dto.response.FinanceScoreResult;
import com.hardwareassistant.hardware_assistant_api.dto.response.InventoryAlertResult;
import com.hardwareassistant.hardware_assistant_api.dto.response.MetricsResult;
import com.hardwareassistant.hardware_assistant_api.exception.BusinessException;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.MerchantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AINarrativeService {

    private final MetricsService             metricsService;
    private final FinanceScoreService        financeScoreService;
    private final InventoryIntelligenceService inventoryService;
    private final MerchantProfileRepository  merchantProfileRepository;
    private final RestTemplate               restTemplate;
    private final ObjectMapper               objectMapper;

    @Value("${app.ai.groq-api-key}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${app.ai.model}")
    private String groqModel;

    public AINarrativeResult generate(User user) {
        MerchantProfile profile = merchantProfileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Please complete your merchant profile first"));

        // ── 1. Compute all data deterministically ─────────────
        MetricsResult        metrics   = metricsService.compute(user);
        FinanceScoreResult   score     = financeScoreService.compute(user);
        InventoryAlertResult inventory = inventoryService.analyze(user);

        // ── 2. Build structured prompt ────────────────────────
        String prompt = buildPrompt(profile, metrics, score, inventory);

        // ── 3. Call Groq ──────────────────────────────────────
        String rawResponse = callGroq(prompt);

        // ── 4. Parse JSON response ────────────────────────────
        return parseResponse(rawResponse);
    }

    // ── Prompt Builder ────────────────────────────────────────
    private String buildPrompt(
            MerchantProfile profile,
            MetricsResult metrics,
            FinanceScoreResult score,
            InventoryAlertResult inventory) {

        String businessName = profile.getBusinessName() != null
                ? profile.getBusinessName() : "the business";
        String location = profile.getLocation() != null
                ? profile.getLocation() : "Nigeria";

        // Top 3 inventory alerts only
        String alertsSummary = "None";
        if (!inventory.getAlerts().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            inventory.getAlerts().stream().limit(3).forEach(a ->
                    sb.append("- [").append(a.getSeverity()).append("] ")
                      .append(a.getMessage()).append("\n"));
            alertsSummary = sb.toString().trim();
        }

        return """
You are a financial advisor for small hardware retail businesses in Nigeria.
You have been given PRE-COMPUTED financial data. You must ONLY narrate and explain these numbers.
DO NOT compute, estimate, or invent any figures. If a value is zero or missing, say so honestly.

BUSINESS CONTEXT:
- Business: %s
- Location: %s

PRE-COMPUTED METRICS (this month):
- Revenue today: ₦%s
- Revenue this week: ₦%s
- Revenue this month: ₦%s
- Revenue last month: ₦%s
- Expenses this month: ₦%s
- Net profit this month: ₦%s
- Profit margin: %s%%
- Month-on-month growth: %s%% (%s)
- Sales count today: %d
- Sales count this month: %d

FINANCE SCORE:
- Score: %d / 100
- Tier: %s
- Lending ready: %s
- Revenue Stability: %d/100 — %s
- Cashflow Health: %d/100 — %s
- Growth Trajectory: %d/100 — %s
- Data Completeness: %d/100 — %s
- Strengths: %s
- Weaknesses: %s

INVENTORY ALERTS (%d total — %d critical, %d warning, %d info):
%s

TASK: Respond ONLY with a valid JSON object. No preamble, no markdown, no explanation outside the JSON.

{
  "executiveSummary": "2-3 sentence overview of the business this month. Be specific with the numbers provided.",
  "revenueNarrative": "Explain the revenue trend and MoM growth in plain language. Mention actual figures.",
  "profitCommentary": "Explain what the profit margin means for this business and what is driving expenses.",
  "inventoryWarning": "Narrate the top inventory issues in plain language. If none, say inventory looks healthy.",
  "financeScoreExplanation": "Explain what the finance score means, why they got it, and one specific thing to improve it.",
  "topActionItems": [
    { "rank": 1, "title": "Short title", "detail": "Specific actionable advice based only on the data above", "impact": "HIGH" },
    { "rank": 2, "title": "Short title", "detail": "Specific actionable advice based only on the data above", "impact": "MEDIUM" },
    { "rank": 3, "title": "Short title", "detail": "Specific actionable advice based only on the data above", "impact": "MEDIUM" }
  ]
}
""".formatted(
                businessName, location,
                metrics.getRevenueToday(),
                metrics.getRevenueThisWeek(),
                metrics.getRevenueThisMonth(),
                metrics.getRevenueLastMonth(),
                metrics.getExpensesThisMonth(),
                metrics.getNetProfitThisMonth(),
                metrics.getProfitMarginPercent(),
                metrics.getMomGrowthPercent(),
                metrics.getGrowthDirection(),
                metrics.getSalesToday(),
                metrics.getSalesThisMonth(),
                score.getScore(),
                score.getTier(),
                score.isLendingReady() ? "Yes" : "Not yet",
                score.getRevenueStability().getRawScore(), score.getRevenueStability().getInsight(),
                score.getCashflowHealth().getRawScore(),   score.getCashflowHealth().getInsight(),
                score.getGrowthTrajectory().getRawScore(), score.getGrowthTrajectory().getInsight(),
                score.getDataCompleteness().getRawScore(), score.getDataCompleteness().getInsight(),
                String.join(", ", score.getStrengths()),
                String.join(", ", score.getWeaknesses()),
                inventory.getTotalAlerts(),
                inventory.getCriticalCount(),
                inventory.getWarningCount(),
                inventory.getInfoCount(),
                alertsSummary
        );
    }

    // ── Groq API Call ─────────────────────────────────────────
    private String callGroq(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", groqModel,
                "temperature", 0.3,   // low temp = factual, consistent
                "max_tokens", 1500,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(groqApiUrl, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException("AI service returned an error. Please try again.");
            }

            JsonNode root    = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("choices").get(0).path("message").path("content");

            if (content.isMissingNode()) {
                throw new BusinessException("AI response was empty. Please try again.");
            }

            return content.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Groq API call failed", e);
            throw new BusinessException("Could not reach AI service. Please try again later.");
        }
    }

    // ── Response Parser ───────────────────────────────────────
    private AINarrativeResult parseResponse(String raw) {
        try {
            // Strip any accidental markdown fences
            String cleaned = raw.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }

            JsonNode node = objectMapper.readTree(cleaned);

            List<ActionItem> actions = new ArrayList<>();
            JsonNode items = node.path("topActionItems");
            if (items.isArray()) {
                items.forEach(item -> actions.add(ActionItem.builder()
                        .rank(item.path("rank").asInt())
                        .title(item.path("title").asText())
                        .detail(item.path("detail").asText())
                        .impact(item.path("impact").asText("MEDIUM"))
                        .build()));
            }

            return AINarrativeResult.builder()
                    .executiveSummary(node.path("executiveSummary").asText())
                    .revenueNarrative(node.path("revenueNarrative").asText())
                    .profitCommentary(node.path("profitCommentary").asText())
                    .inventoryWarning(node.path("inventoryWarning").asText())
                    .financeScoreExplanation(node.path("financeScoreExplanation").asText())
                    .topActionItems(actions)
                    .generatedAt(Instant.now().toString())
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse AI narrative response", e);
            throw new BusinessException("AI returned an unexpected format. Please try again.");
        }
    }
}
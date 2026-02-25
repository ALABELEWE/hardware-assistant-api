package com.hardwareassistant.hardware_assistant_api.service.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.security.AiResponseValidator;
import com.hardwareassistant.hardware_assistant_api.security.InputSanitizer;
import com.hardwareassistant.hardware_assistant_api.security.SecurePromptBuilder;
import com.hardwareassistant.hardware_assistant_api.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    @Value("${app.ai.groq-api-key}")
    private String apiKey;

    @Value("${app.ai.groq-base-url}")
    private String baseUrl;

    @Value("${app.ai.model}")
    private String model;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final InputSanitizer inputSanitizer;
    private final SecurePromptBuilder promptBuilder;
    private final AiResponseValidator responseValidator;

    private static final int MAX_RETRIES = 2;

    @Override
    public String generateBusinessInsights(MerchantProfile profile) {

        // Step 1 — Sanitize all merchant inputs
        try {
            inputSanitizer.sanitizeMerchantProfile(
                    profile.getBusinessName(),
                    profile.getLocation(),
                    profile.getProducts(),
                    profile.getProducts() // no description field — reuse products
            );
        } catch (SecurityException e) {
            log.warn("SECURITY - Input sanitization blocked request for: {} - reason: {}",
                    profile.getBusinessName(), e.getMessage());
            return fallbackJson();
        }

        // Step 2 — Build secure prompts using actual model fields
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(
                profile.getBusinessName(),
                profile.getLocation()      != null ? profile.getLocation()      : "Not specified",
                profile.getProducts()      != null ? profile.getProducts()       : "Not specified",
                "",
                profile.getPriceRange()    != null ? profile.getPriceRange()     : "Not specified",
                profile.getCustomerType()  != null ? profile.getCustomerType()   : "Not specified"
        );

        // Step 3 — Call Groq with retry on validation failure
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String rawJson = callGroq(systemPrompt, userPrompt);

                // Step 4 — Validate AI response structure and content
                JsonNode validated = responseValidator.validateAndParse(rawJson);
                log.info("Groq analysis generated and validated for: {}", profile.getBusinessName());
                return objectMapper.writeValueAsString(validated);

            } catch (IllegalStateException e) {
                log.warn("AI response validation failed on attempt {}/{}: {}",
                        attempt, MAX_RETRIES, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("AI failed after {} attempts for: {}",
                            MAX_RETRIES, profile.getBusinessName());
                    return fallbackJson();
                }
            } catch (Exception e) {
                log.error("Groq API error for {}: {}", profile.getBusinessName(), e.getMessage());
                return fallbackJson();
            }
        }

        return fallbackJson();
    }

    private String callGroq(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user",   "content", userPrompt)
                ),
                "temperature", 0.7,
                "max_tokens", 1024,
                "response_format", Map.of("type", "json_object")
        );

        Map<?, ?> response = webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .post()
                .uri("/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<?> choices    = (List<?>) response.get("choices");
        Map<?, ?> choice   = (Map<?, ?>)  choices.get(0);
        Map<?, ?> message  = (Map<?, ?>)  choice.get("message");
        return (String) message.get("content");
    }

    private String fallbackJson() {
        return """
            {
              "summary": "Analysis temporarily unavailable. Please try again.",
              "strengths": [],
              "weaknesses": [],
              "recommendations": ["Please retry the analysis in a few minutes"],
              "marketOpportunities": [],
              "estimatedMonthlyRevenuePotential": "N/A",
              "smsAlert": "Your HardwareAI analysis is being processed. Please check the app."
            }
            """;
    }
}
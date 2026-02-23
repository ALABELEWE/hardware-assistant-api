package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
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

    @Override
    public String generateBusinessInsights(MerchantProfile profile) {

        // Groq uses OpenAI-compatible format
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", """
                        You are a business intelligence assistant for Lagos hardware merchants.
                        Always respond ONLY with valid raw JSON.
                        No markdown, no code fences, no explanation — just the JSON object.
                        """
                        ),
                        Map.of(
                                "role", "user",
                                "content", buildPrompt(profile)
                        )
                ),
                "temperature", 0.7,
                "max_tokens", 1024,
                "response_format", Map.of("type", "json_object")
        );

        try {
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

            // Groq response: choices[0].message.content
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            String rawJson = (String) message.get("content");

            // Validate it's real JSON
            objectMapper.readTree(rawJson);

            log.info("Groq analysis generated for: {}", profile.getBusinessName());
            return rawJson;

        } catch (Exception e) {
            log.error(" Groq API error for {}: {}", profile.getBusinessName(), e.getMessage());
            return fallbackJson();
        }
    }

    private String buildPrompt(MerchantProfile profile) {
        return String.format("""
                Analyze this Lagos hardware merchant and return ONLY a JSON object \
                with this exact structure:
                {
                  "summary": "2-3 sentence business summary",
                  "strengths": ["strength1", "strength2", "strength3"],
                  "weaknesses": ["weakness1", "weakness2"],
                  "recommendations": ["action1", "action2", "action3", "action4"],
                  "marketOpportunities": ["opportunity1", "opportunity2"],
                  "estimatedMonthlyRevenuePotential": "₦X,XXX,XXX – ₦X,XXX,XXX",
                  "smsAlert": "Max 160 chars SMS with top recommendation"
                }

                Business details:
                - Business Name: %s
                - Location: %s
                - Customer Type: %s
                - Products: %s
                - Price Range: %s
                """,
                profile.getBusinessName(),
                profile.getLocation(),
                profile.getCustomerType(),
                profile.getProducts(),
                profile.getPriceRange()
        );
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


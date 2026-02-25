package com.hardwareassistant.hardware_assistant_api.security;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class AiResponseValidator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> REQUIRED_KEYS = Set.of(
            "summary", "strengths", "weaknesses"
            ,"recommendations", "marketOpportunities", "estimatedMonthlyRevenuePotential", "smsAlert"
    );

    private static final Set<String> ARRAY_KEYS = Set.of(
            "strengths", "weaknesses", "recommendations", "marketOpportunities"
    );

    private static final List<String> SUSPICIOUS_CONTENT = List.of(
            "system prompt", "api key", "secret", "ignore instructions",
            "jailbreak", "I cannot", "As an AI", "I am not able to",
            "hacked", "override", "developer mode"
    );

    private static final int MAX_SUMMARY_LENGTH = 500;
    private static final int MAX_SMS_LENGTH = 160;
    private static final int MAX_ARRAY_ITEMS = 10;

    public JsonNode validateAndParse(String rawResponse) {
        // Strip markdown code blocks if present
        String cleaned = rawResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        // Extract JSON if wrapped in text
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start == -1 || end == -1) {
            log.error("AI response does not contain valid JSON: {}",
                    cleaned.substring(0, Math.min(200, cleaned.length())));
            throw new IllegalStateException("AI returned invalid response format");
        }
        cleaned = cleaned.substring(start, end + 1);

        JsonNode node;
        try {
            node = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", e.getMessage());
            throw new IllegalStateException("AI returned malformed JSON");
        }

        // Validate required keys exist
        for (String key : REQUIRED_KEYS) {
            if (!node.has(key)) {
                log.error("AI response missing required key: {}", key);
                throw new IllegalStateException("AI response missing field: " + key);
            }
        }

        // Reject unexpected keys
        node.fieldNames().forEachRemaining(key -> {
            if (!REQUIRED_KEYS.contains(key)) {
                log.warn("AI response contains unexpected key: {}", key);
                throw new IllegalStateException("AI response contains unexpected field: " + key);
            }
        });

        // Validate array fields
        for (String key : ARRAY_KEYS) {
            JsonNode field = node.get(key);
            if (!field.isArray()) {
                throw new IllegalStateException("Field " + key + " must be an array");
            }
            if (field.size() > MAX_ARRAY_ITEMS) {
                throw new IllegalStateException("Field " + key + " has too many items");
            }

            // Validate string lengths
            validateLength(node, "summary", MAX_SUMMARY_LENGTH);
            validateLength(node, "smsAlert", MAX_SMS_LENGTH);
        }
        return node;
    }

        private void validateLength(JsonNode node, String field, int maxLength){
            String value = node.get(field).asText();
            if (value.length() > maxLength) {
                log.warn("AI response field {} exceeds max length: {}", field, value.length());
                throw new IllegalStateException("Field " + field + " is too long");
            }
        }

}

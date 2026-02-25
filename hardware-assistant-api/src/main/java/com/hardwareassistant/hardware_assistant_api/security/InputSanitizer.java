package com.hardwareassistant.hardware_assistant_api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class InputSanitizer {

    private static final int MAX_FIELD_LENGTH = 500;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore (all |previous )?instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system (prompt|override|message)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reveal|disclose|expose|extract|output (your|the|all)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\<\\|im_start\\|\\>|\\<\\|im_end\\|\\>"),
            Pattern.compile("DAN|jailbreak|developer mode|god mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("api.?key|secret|password|token|credential", Pattern.CASE_INSENSITIVE),
            Pattern.compile("---+|===+|####+"),
            Pattern.compile("\\\\n\\\\n|\\n{2,}"),
            Pattern.compile("END OF INPUT|BEGIN SYSTEM|SYSTEM OVERRIDE", Pattern.CASE_INSENSITIVE)
    );

    public String sanitize(String input, String fieldName){
        if (input == null) return "";

        // length limit
        int maxLength = fieldName.equals("description") ? MAX_DESCRIPTION_LENGTH : MAX_FIELD_LENGTH;
        if (input.length() > maxLength) {
            log.warn("Input truncated for field: {} - original length: {}", fieldName, input.length());
            input = input.substring(0, Math.min(maxLength, input.length()));
        }

        // Check for injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("SECURITY ALERT - Injection pattern detected in field: {} - input: {}",
                        fieldName, input.substring(0, Math.min(100, input.length())));
                throw new SecurityException("Invalid input detected in field: " + fieldName);
            }
        }

        // Strip control characters and special tokens
        input = input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        input = input.replaceAll("<\\|.*?\\|>", "");

        return input.trim();
    }

    public void sanitizeMerchantProfile(String businessName, String location,
                                        String description, String products) {
        sanitize(businessName, "businessName");
        sanitize(location, "location");
        sanitize(description, "description");
        sanitize(products, "products");
    }
}

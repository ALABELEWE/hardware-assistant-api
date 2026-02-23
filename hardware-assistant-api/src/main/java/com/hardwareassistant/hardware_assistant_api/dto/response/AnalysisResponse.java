package com.hardwareassistant.hardware_assistant_api.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareassistant.hardware_assistant_api.model.BusinessAnalysis;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Slf4j
public class AnalysisResponse {

    private UUID id;
    private Map<String, Object> analysis;
    private LocalDateTime createdAt;

    private static final ObjectMapper mapper = new ObjectMapper();

    public static AnalysisResponse from(BusinessAnalysis entity) {
        AnalysisResponse dto = new AnalysisResponse();
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        try {
            dto.setAnalysis(mapper.readValue(
                    entity.getAiResponseJson(),
                    new TypeReference<Map<String, Object>>() {}
            ));
        } catch (Exception e) {
            log.warn("Could not parse aiResponseJson for analysis {}", entity.getId());
            dto.setAnalysis(Map.of("error", "Could not parse analysis"));
        }
        return dto;
    }
}
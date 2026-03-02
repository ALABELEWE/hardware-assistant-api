package com.hardwareassistant.hardware_assistant_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AINarrativeResult {

    private String executiveSummary;
    private String revenueNarrative;
    private String profitCommentary;
    private String inventoryWarning;
    private String financeScoreExplanation;
    private List<ActionItem> topActionItems;
    private String generatedAt;  // ISO timestamp

    @Data
    @Builder
    public static class ActionItem {
        private int    rank;
        private String title;
        private String detail;
        private String impact;   // "HIGH" | "MEDIUM" | "LOW"
    }
}
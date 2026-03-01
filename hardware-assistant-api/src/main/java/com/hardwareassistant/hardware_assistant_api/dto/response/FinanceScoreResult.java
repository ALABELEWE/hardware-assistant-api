package com.hardwareassistant.hardware_assistant_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FinanceScoreResult {

    // ── Overall ───────────────────────────────────────────────
    private int    score;           // 0–100
    private String tier;            // "Poor" | "Fair" | "Good" | "Excellent"
    private String tierColor;       // "red" | "amber" | "green" | "emerald"
    private String summary;         // 1-sentence plain-English verdict

    // ── Component Breakdown ───────────────────────────────────
    private ScoreComponent revenueStability;
    private ScoreComponent cashflowHealth;
    private ScoreComponent growthTrajectory;
    private ScoreComponent dataCompleteness;

    // ── Actionable Flags ──────────────────────────────────────
    private List<String> strengths;   // what's working
    private List<String> weaknesses;  // what to fix
    private boolean      lendingReady; // score >= 60

    @Data
    @Builder
    public static class ScoreComponent {
        private String name;
        private int    rawScore;      // 0–100 for this component
        private int    weightedScore; // rawScore * weight
        private int    weight;        // percentage weight e.g. 30
        private String insight;       // one-line explanation
    }
}
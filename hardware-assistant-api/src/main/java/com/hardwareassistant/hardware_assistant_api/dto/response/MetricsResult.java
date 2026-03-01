package com.hardwareassistant.hardware_assistant_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MetricsResult {

    // ── Revenue ───────────────────────────────────────────────
    private BigDecimal revenueToday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueLastMonth;

    // ── Profit ────────────────────────────────────────────────
    private BigDecimal expensesThisMonth;
    private BigDecimal netProfitThisMonth;
    private BigDecimal profitMarginPercent;   // (net / gross) * 100

    // ── Growth ───────────────────────────────────────────────
    private BigDecimal momGrowthPercent;      // month-on-month revenue growth %
    private String     growthDirection;       // "up", "down", "flat"

    // ── Expense Breakdown ─────────────────────────────────────
    private List<ExpenseCategory> expenseBreakdown;

    // ── Transaction Counts ────────────────────────────────────
    private long salesToday;
    private long salesThisMonth;

    @Data
    @Builder
    public static class ExpenseCategory {
        private String     category;
        private BigDecimal amount;
        private BigDecimal percentOfTotal;   // this category / total expenses * 100
    }
}
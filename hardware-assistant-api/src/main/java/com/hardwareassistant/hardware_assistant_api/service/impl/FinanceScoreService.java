package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.dto.response.FinanceScoreResult;
import com.hardwareassistant.hardware_assistant_api.dto.response.FinanceScoreResult.ScoreComponent;
import com.hardwareassistant.hardware_assistant_api.exception.BusinessException;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.ExpenseRecordRepository;
import com.hardwareassistant.hardware_assistant_api.repository.MerchantProfileRepository;
import com.hardwareassistant.hardware_assistant_api.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceScoreService {

    private final SalesTransactionRepository salesRepo;
    private final ExpenseRecordRepository    expenseRepo;
    private final MerchantProfileRepository  merchantProfileRepository;

    // ── Weights (must sum to 100) ─────────────────────────────
    private static final int W_REVENUE_STABILITY  = 30;
    private static final int W_CASHFLOW_HEALTH    = 25;
    private static final int W_GROWTH_TRAJECTORY  = 25;
    private static final int W_DATA_COMPLETENESS  = 20;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Transactional(readOnly = true)
    public FinanceScoreResult compute(User user) {
        MerchantProfile profile = merchantProfileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Please complete your merchant profile first"));

        LocalDate today = LocalDate.now();

        // ── Pull 3 months of revenue ──────────────────────────
        LocalDate m0Start = today.withDayOfMonth(1);                       // this month
        LocalDate m1Start = m0Start.minusMonths(1);                        // last month
        LocalDate m2Start = m0Start.minusMonths(2);                        // 2 months ago
        LocalDate m1End   = m0Start.minusDays(1);
        LocalDate m2End   = m1Start.minusDays(1);

        BigDecimal rev0 = salesRepo.sumRevenueByPeriod(profile, m0Start, today);
        BigDecimal rev1 = salesRepo.sumRevenueByPeriod(profile, m1Start, m1End);
        BigDecimal rev2 = salesRepo.sumRevenueByPeriod(profile, m2Start, m2End);

        BigDecimal exp0 = expenseRepo.sumExpensesByPeriod(profile, m0Start, today);

        // ── 1. Revenue Stability (30%) ────────────────────────
        // Measures: do they have consistent revenue across months?
        // Method: coefficient of variation (lower = more stable = higher score)
        int stabilityScore = computeStabilityScore(rev0, rev1, rev2);
        String stabilityInsight = buildStabilityInsight(rev0, rev1, rev2, stabilityScore);

        // ── 2. Cashflow Health (25%) ──────────────────────────
        // Measures: profit margin this month
        int cashflowScore = computeCashflowScore(rev0, exp0);
        String cashflowInsight = buildCashflowInsight(rev0, exp0, cashflowScore);

        // ── 3. Growth Trajectory (25%) ────────────────────────
        // Measures: MoM revenue growth trend over 3 months
        int growthScore = computeGrowthScore(rev0, rev1, rev2);
        String growthInsight = buildGrowthInsight(rev0, rev1, rev2, growthScore);

        // ── 4. Data Completeness (20%) ────────────────────────
        // Measures: how many of the last 3 months have ANY data logged
        int completenessScore = computeCompletenessScore(rev0, rev1, rev2, exp0);
        String completenessInsight = buildCompletenessInsight(completenessScore);

        // ── Weighted total ────────────────────────────────────
        int totalScore = Math.round(
                (stabilityScore  * W_REVENUE_STABILITY  / 100f) +
                (cashflowScore   * W_CASHFLOW_HEALTH    / 100f) +
                (growthScore     * W_GROWTH_TRAJECTORY  / 100f) +
                (completenessScore * W_DATA_COMPLETENESS / 100f)
        );
        totalScore = Math.max(0, Math.min(100, totalScore));

        // ── Tier ──────────────────────────────────────────────
        String tier      = getTier(totalScore);
        String tierColor = getTierColor(totalScore);
        String summary   = buildSummary(totalScore, tier, profile);

        // ── Strengths & Weaknesses ────────────────────────────
        List<String> strengths  = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        if (stabilityScore  >= 70) strengths.add("Consistent monthly revenue");
        else                       weaknesses.add("Revenue varies too much month to month");

        if (cashflowScore   >= 70) strengths.add("Healthy profit margins");
        else if (cashflowScore >= 40) weaknesses.add("Profit margin is thin — review expenses");
        else                       weaknesses.add("Spending exceeds or nearly matches revenue");

        if (growthScore     >= 70) strengths.add("Strong revenue growth trend");
        else if (growthScore < 40) weaknesses.add("Revenue is declining or stagnant");

        if (completenessScore >= 80) strengths.add("Regular data entry habits");
        else                         weaknesses.add("Log sales and expenses more consistently");

        log.info("Finance score computed for {}: {} ({})", user.getEmail(), totalScore, tier);

        return FinanceScoreResult.builder()
                .score(totalScore)
                .tier(tier)
                .tierColor(tierColor)
                .summary(summary)
                .revenueStability(ScoreComponent.builder()
                        .name("Revenue Stability")
                        .rawScore(stabilityScore)
                        .weightedScore(Math.round(stabilityScore * W_REVENUE_STABILITY / 100f))
                        .weight(W_REVENUE_STABILITY)
                        .insight(stabilityInsight)
                        .build())
                .cashflowHealth(ScoreComponent.builder()
                        .name("Cashflow Health")
                        .rawScore(cashflowScore)
                        .weightedScore(Math.round(cashflowScore * W_CASHFLOW_HEALTH / 100f))
                        .weight(W_CASHFLOW_HEALTH)
                        .insight(cashflowInsight)
                        .build())
                .growthTrajectory(ScoreComponent.builder()
                        .name("Growth Trajectory")
                        .rawScore(growthScore)
                        .weightedScore(Math.round(growthScore * W_GROWTH_TRAJECTORY / 100f))
                        .weight(W_GROWTH_TRAJECTORY)
                        .insight(growthInsight)
                        .build())
                .dataCompleteness(ScoreComponent.builder()
                        .name("Data Completeness")
                        .rawScore(completenessScore)
                        .weightedScore(Math.round(completenessScore * W_DATA_COMPLETENESS / 100f))
                        .weight(W_DATA_COMPLETENESS)
                        .insight(completenessInsight)
                        .build())
                .strengths(strengths)
                .weaknesses(weaknesses)
                .lendingReady(totalScore >= 60)
                .build();
    }

    // ── Scoring Methods ───────────────────────────────────────

    private int computeStabilityScore(BigDecimal r0, BigDecimal r1, BigDecimal r2) {
        // Need at least 2 months of data
        long nonZero = countNonZero(r0, r1, r2);
        if (nonZero < 2) return 30; // penalise for insufficient history

        double mean = (r0.doubleValue() + r1.doubleValue() + r2.doubleValue()) / 3.0;
        if (mean == 0) return 0;

        double variance = (
                Math.pow(r0.doubleValue() - mean, 2) +
                Math.pow(r1.doubleValue() - mean, 2) +
                Math.pow(r2.doubleValue() - mean, 2)
        ) / 3.0;

        double cv = Math.sqrt(variance) / mean; // coefficient of variation (0 = perfect stability)

        // CV of 0 → score 100, CV of 1+ → score 0
        int score = (int) Math.round(Math.max(0, 100 - (cv * 100)));
        return Math.min(100, score);
    }

    private int computeCashflowScore(BigDecimal revenue, BigDecimal expenses) {
        if (revenue.compareTo(BigDecimal.ZERO) == 0) return 0;

        // Profit margin % = (revenue - expenses) / revenue * 100
        BigDecimal margin = revenue.subtract(expenses)
                .multiply(HUNDRED)
                .divide(revenue, 2, RoundingMode.HALF_UP);

        double m = margin.doubleValue();

        // Score bands:
        // margin >= 40%  → 100
        // margin 20-40%  → 60–99
        // margin 0-20%   → 20–59
        // margin < 0     → 0–19
        if (m >= 40) return 100;
        if (m >= 20) return (int) (60 + ((m - 20) / 20.0) * 39);
        if (m >= 0)  return (int) (20 + (m / 20.0) * 39);
        return Math.max(0, (int) (20 + m)); // negative margin: 0–19
    }

    private int computeGrowthScore(BigDecimal r0, BigDecimal r1, BigDecimal r2) {
        // Score based on MoM trend across 3 months
        // Best case: r2 < r1 < r0 (consistent growth)
        int score = 50; // neutral baseline

        if (r1.compareTo(BigDecimal.ZERO) > 0) {
            double momLatest = (r0.doubleValue() - r1.doubleValue()) / r1.doubleValue() * 100;
            // +5 points per 10% growth, -5 per 10% decline, capped at ±50
            score += Math.max(-50, Math.min(50, (int)(momLatest / 10.0) * 5));
        }

        if (r2.compareTo(BigDecimal.ZERO) > 0 && r1.compareTo(BigDecimal.ZERO) > 0) {
            double momPrior = (r1.doubleValue() - r2.doubleValue()) / r2.doubleValue() * 100;
            score += Math.max(-20, Math.min(20, (int)(momPrior / 10.0) * 2));
        }

        return Math.max(0, Math.min(100, score));
    }

    private int computeCompletenessScore(BigDecimal r0, BigDecimal r1, BigDecimal r2, BigDecimal exp0) {
        int points = 0;
        // 25 points per month with sales data (3 months = 75 points)
        if (r0.compareTo(BigDecimal.ZERO) > 0) points += 25;
        if (r1.compareTo(BigDecimal.ZERO) > 0) points += 25;
        if (r2.compareTo(BigDecimal.ZERO) > 0) points += 25;
        // 25 points for logging expenses this month
        if (exp0.compareTo(BigDecimal.ZERO) > 0) points += 25;
        return points;
    }

    // ── Insight Builders ──────────────────────────────────────

    private String buildStabilityInsight(BigDecimal r0, BigDecimal r1, BigDecimal r2, int score) {
        if (countNonZero(r0, r1, r2) < 2) return "Not enough monthly data yet — keep logging sales";
        if (score >= 80) return "Revenue is very consistent across the past 3 months";
        if (score >= 60) return "Some fluctuation in monthly revenue — normal for retail";
        if (score >= 40) return "Revenue swings significantly month to month";
        return "Highly inconsistent revenue — investigate slow months";
    }

    private String buildCashflowInsight(BigDecimal rev, BigDecimal exp, int score) {
        if (rev.compareTo(BigDecimal.ZERO) == 0) return "No revenue recorded this month";
        if (score >= 80) return "Strong profit margin — business is financially healthy";
        if (score >= 60) return "Decent margin — small improvements to expenses could help";
        if (score >= 40) return "Thin margin — expenses are eating into profit";
        return "Expenses are very high relative to revenue";
    }

    private String buildGrowthInsight(BigDecimal r0, BigDecimal r1, BigDecimal r2, int score) {
        if (r1.compareTo(BigDecimal.ZERO) == 0) return "No prior month data to measure growth";
        if (score >= 70) return "Revenue is growing — strong positive trend";
        if (score >= 50) return "Revenue is roughly stable month on month";
        return "Revenue is declining — review pricing and sales strategy";
    }

    private String buildCompletenessInsight(int score) {
        if (score == 100) return "Excellent — sales and expenses logged across all 3 months";
        if (score >= 75)  return "Good data habits — keep logging consistently";
        if (score >= 50)  return "Partial data — missing some months reduces score accuracy";
        return "Very little data logged — score accuracy is low";
    }

    private String buildSummary(int score, String tier, MerchantProfile profile) {
        String biz = profile.getBusinessName() != null ? profile.getBusinessName() : "Your business";
        if (score >= 80) return biz + " shows strong financial health and is well-positioned for credit.";
        if (score >= 60) return biz + " meets basic lending criteria with room to improve.";
        if (score >= 40) return biz + " needs stronger cashflow consistency before approaching lenders.";
        return biz + " has insufficient or inconsistent financial data — focus on data logging first.";
    }

    // ── Tier Helpers ──────────────────────────────────────────

    private String getTier(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Fair";
        return "Poor";
    }

    private String getTierColor(int score) {
        if (score >= 80) return "emerald";
        if (score >= 60) return "green";
        if (score >= 40) return "amber";
        return "red";
    }

    private long countNonZero(BigDecimal... values) {
        long count = 0;
        for (BigDecimal v : values) if (v.compareTo(BigDecimal.ZERO) > 0) count++;
        return count;
    }
}
package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.dto.response.MetricsResult;
import com.hardwareassistant.hardware_assistant_api.dto.response.MetricsResult.ExpenseCategory;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final SalesTransactionRepository  salesRepo;
    private final ExpenseRecordRepository     expenseRepo;
    private final MerchantProfileRepository   merchantProfileRepository;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Transactional(readOnly = true)
    public MetricsResult compute(User user) {
        MerchantProfile profile = merchantProfileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Please complete your merchant profile first"));

        LocalDate today     = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        // ── Period boundaries ─────────────────────────────────
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisMonthEnd   = today;

        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd   = thisMonthStart.minusDays(1);

        // ── Revenue queries ───────────────────────────────────
        BigDecimal revenueToday     = salesRepo.sumRevenueByPeriod(profile, today, today);
        BigDecimal revenueThisWeek  = salesRepo.sumRevenueByPeriod(profile, weekStart, today);
        BigDecimal revenueThisMonth = salesRepo.sumRevenueByPeriod(profile, thisMonthStart, thisMonthEnd);
        BigDecimal revenueLastMonth = salesRepo.sumRevenueByPeriod(profile, lastMonthStart, lastMonthEnd);

        // ── Expense queries ───────────────────────────────────
        BigDecimal expensesThisMonth = expenseRepo.sumExpensesByPeriod(profile, thisMonthStart, thisMonthEnd);

        // ── Derived: net profit ───────────────────────────────
        BigDecimal netProfit = revenueThisMonth.subtract(expensesThisMonth);

        // ── Derived: profit margin % ──────────────────────────
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (revenueThisMonth.compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = netProfit
                    .multiply(HUNDRED)
                    .divide(revenueThisMonth, 2, RoundingMode.HALF_UP);
        }

        // ── Derived: MoM growth % ─────────────────────────────
        BigDecimal momGrowth = BigDecimal.ZERO;
        String growthDirection = "flat";
        if (revenueLastMonth.compareTo(BigDecimal.ZERO) > 0) {
            momGrowth = revenueThisMonth
                    .subtract(revenueLastMonth)
                    .multiply(HUNDRED)
                    .divide(revenueLastMonth, 2, RoundingMode.HALF_UP);
        } else if (revenueThisMonth.compareTo(BigDecimal.ZERO) > 0) {
            // No last month data but has this month — treat as 100% growth
            momGrowth = HUNDRED;
        }

        if (momGrowth.compareTo(BigDecimal.ZERO) > 0)       growthDirection = "up";
        else if (momGrowth.compareTo(BigDecimal.ZERO) < 0)  growthDirection = "down";

        // ── Expense breakdown by category ─────────────────────
        List<Object[]> rawBreakdown = expenseRepo.sumByCategory(profile, thisMonthStart, thisMonthEnd);
        List<ExpenseCategory> breakdown = rawBreakdown.stream()
                .map(row -> {
                    String     cat    = (String)     row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    BigDecimal pct    = expensesThisMonth.compareTo(BigDecimal.ZERO) > 0
                            ? amount.multiply(HUNDRED).divide(expensesThisMonth, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return ExpenseCategory.builder()
                            .category(cat)
                            .amount(amount)
                            .percentOfTotal(pct)
                            .build();
                })
                .collect(Collectors.toList());

        // ── Transaction counts ────────────────────────────────
        long salesToday     = salesRepo.countByPeriod(profile, today, today);
        long salesThisMonth = salesRepo.countByPeriod(profile, thisMonthStart, thisMonthEnd);

        log.debug("Metrics computed for merchant: {} | Revenue this month: {} | Margin: {}%",
                user.getEmail(), revenueThisMonth, profitMargin);

        return MetricsResult.builder()
                .revenueToday(revenueToday)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .revenueLastMonth(revenueLastMonth)
                .expensesThisMonth(expensesThisMonth)
                .netProfitThisMonth(netProfit)
                .profitMarginPercent(profitMargin)
                .momGrowthPercent(momGrowth)
                .growthDirection(growthDirection)
                .expenseBreakdown(breakdown)
                .salesToday(salesToday)
                .salesThisMonth(salesThisMonth)
                .build();
    }
}
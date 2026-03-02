package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.dto.response.DataReadinessResult;
import com.hardwareassistant.hardware_assistant_api.exception.BusinessException;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.ExpenseRecordRepository;
import com.hardwareassistant.hardware_assistant_api.repository.MerchantProfileRepository;
import com.hardwareassistant.hardware_assistant_api.repository.ProductRepository;
import com.hardwareassistant.hardware_assistant_api.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DataReadinessService {

    private final SalesTransactionRepository salesRepo;
    private final ExpenseRecordRepository    expenseRepo;
    private final ProductRepository          productRepository;
    private final MerchantProfileRepository  merchantProfileRepository;

    @Transactional(readOnly = true)
    public DataReadinessResult check(User user) {
        MerchantProfile profile = merchantProfileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Merchant profile not found"));

        LocalDate today          = LocalDate.now();
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd   = thisMonthStart.minusDays(1);

        // ── Counts ────────────────────────────────────────────
        long salesThisMonth = salesRepo.countByPeriod(profile, thisMonthStart, today);
        long salesLastMonth = salesRepo.countByPeriod(profile, lastMonthStart, lastMonthEnd);
        long totalProducts  = productRepository
                .findByMerchantProfileAndActiveOrderByNameAsc(profile, true).size();

        BigDecimal expensesThisMonth = expenseRepo
                .sumExpensesByPeriod(profile, thisMonthStart, today);
        boolean hasExpenses = expensesThisMonth.compareTo(BigDecimal.ZERO) > 0;

        boolean hasAnySales = (salesThisMonth + salesLastMonth) > 0;

        // ── Readiness Level ───────────────────────────────────
        // NONE    → no sales at all
        // PARTIAL → has some sales but missing expenses or products
        // READY   → has sales this month + at least some expenses logged
        String readinessLevel;
        String message;

        if (!hasAnySales) {
            readinessLevel = "NONE";
            message = "Start by logging your first sale in the Data Entry tab. "
                    + "The more data you log, the more accurate your AI insights will be.";
        } else if (salesThisMonth == 0) {
            readinessLevel = "PARTIAL";
            message = "You have historical data but no sales logged this month yet. "
                    + "Log today's sales to unlock your AI Business Narrative.";
        } else if (!hasExpenses) {
            readinessLevel = "PARTIAL";
            message = "Great — you have " + salesThisMonth + " sale"
                    + (salesThisMonth > 1 ? "s" : "") + " logged this month. "
                    + "Log at least one expense to get a complete profit analysis.";
        } else {
            readinessLevel = "READY";
            message = "Your data is ready. Generate your AI Business Narrative below.";
        }

        return DataReadinessResult.builder()
                .hasData(hasAnySales)
                .salesThisMonth((int) salesThisMonth)
                .salesLastMonth((int) salesLastMonth)
                .totalProducts((int) totalProducts)
                .hasExpenses(hasExpenses)
                .readinessLevel(readinessLevel)
                .message(message)
                .build();
    }
}
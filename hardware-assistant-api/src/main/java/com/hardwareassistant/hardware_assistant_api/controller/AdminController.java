package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.UserResponse;
import com.hardwareassistant.hardware_assistant_api.model.AiUsage;
import com.hardwareassistant.hardware_assistant_api.repository.AiUsageRepository;
import com.hardwareassistant.hardware_assistant_api.repository.UserRepository;
import com.hardwareassistant.hardware_assistant_api.service.AdminService;
import com.hardwareassistant.hardware_assistant_api.service.AiUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;
    private final AiUsageService aiUsageService;
    private final AiUsageRepository aiUsageRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> users() {
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/ai-usage")
    public ResponseEntity<ApiResponse<Object>> getAiUsage() {

        // ── Per-user breakdown ──────────────────────────────────────────
        List<Object[]> rawUsage = aiUsageRepository.getPerUserUsageSummary();
        List<Map<String, Object>> perUserUsage = rawUsage.stream().map(row -> {
            Map<String, Object> u = new HashMap<>();
            u.put("merchantId", row[0]);
            u.put("email",      row[1]);
            u.put("totalTokens", row[2]);
            u.put("totalCost",   row[3]);
            u.put("analyses",    row[4] != null ? row[4] : 0);
            return u;
        }).toList();

        // ── Platform totals ─────────────────────────────────────────────
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        LocalDateTime monthStart   = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        BigDecimal totalCostAllTime   = aiUsageRepository.totalPlatformCostSince(sixMonthsAgo);
        BigDecimal totalCostThisMonth = aiUsageRepository.totalPlatformCostSince(monthStart);
        Long totalTokensAllTime       = aiUsageRepository.sumTokensByPeriod(sixMonthsAgo);
        Long monthlyAnalyses          = aiUsageRepository.countAnalysesSince(monthStart);

        // ── Monthly trend (last 6 months) ───────────────────────────────
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = LocalDateTime.now().minusMonths(i)
                    .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1);

            BigDecimal monthlyCost   = aiUsageRepository.totalPlatformCostBetween(start, end);
            Long monthlyTokens       = aiUsageRepository.sumTokensBetween(start, end);
            Long monthlyCount        = aiUsageRepository.countAnalysesBetween(start, end);

            String monthName = start.getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            Map<String, Object> month = new HashMap<>();
            month.put("month",    monthName);
            month.put("cost",     monthlyCost   != null ? monthlyCost   : BigDecimal.ZERO);
            month.put("tokens",   monthlyTokens != null ? monthlyTokens : 0L);
            month.put("analyses", monthlyCount  != null ? monthlyCount  : 0L);
            monthlyTrend.add(month);
        }

        // ── Assemble response ───────────────────────────────────────────
        Map<String, Object> result = new HashMap<>();
        result.put("totalCost",        totalCostAllTime  != null ? totalCostAllTime  : BigDecimal.ZERO);
        result.put("totalTokens",      totalTokensAllTime != null ? totalTokensAllTime : 0L);
        result.put("totalCostThisMonth", totalCostThisMonth != null ? totalCostThisMonth : BigDecimal.ZERO);
        result.put("monthlyAnalyses",  monthlyAnalyses   != null ? monthlyAnalyses   : 0L);
        result.put("perUserUsage",     perUserUsage);
        result.put("monthlyTrend",     monthlyTrend);

        return ResponseEntity.ok(ApiResponse.success("AI usage retrieved", result));
    }
}
package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.UserResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<Object[]> usage = aiUsageRepository.getPerUserUsageSummary();
        BigDecimal totalCost = aiUsageRepository.totalPlatformCostSince(
                LocalDateTime.now().minusMonths(1));

        Map<String, Object> result = new HashMap<>();
        result.put("totalPlatformCostThisMonth", totalCost != null ? totalCost : BigDecimal.ZERO);
        result.put("perUserUsage", usage.stream().map(row -> {
            Map<String, Object> u = new HashMap<>();
            u.put("merchantId",    row[0]);
            u.put("email",         row[1]);
            u.put("totalTokens",   row[2]);
            u.put("totalCost",     row[3]);
            return u;
        }).toList());

        return ResponseEntity.ok(ApiResponse.success("AI usage retrieved", result));
    }
}

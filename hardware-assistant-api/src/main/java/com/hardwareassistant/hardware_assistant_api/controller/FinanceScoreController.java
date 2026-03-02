package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.FinanceScoreResult;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.service.impl.FinanceScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance-score")
@RequiredArgsConstructor
public class FinanceScoreController {

    private final FinanceScoreService financeScoreService;

    @GetMapping
    public ResponseEntity<ApiResponse<FinanceScoreResult>> getScore(
            @AuthenticationPrincipal User user) {
        FinanceScoreResult score = financeScoreService.compute(user);
        return ResponseEntity.ok(ApiResponse.success(score));
    }
}
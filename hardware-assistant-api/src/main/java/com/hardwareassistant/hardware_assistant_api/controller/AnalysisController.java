package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.AnalysisResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AnalysisResponse>> generate(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "false") boolean sendSms) {
        AnalysisResponse response = AnalysisResponse
                .from(analysisService.generateAnalysis(user, sendSms));
        return ResponseEntity.ok(ApiResponse.success("Analysis generated", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<AnalysisResponse>>> history(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AnalysisResponse> response = analysisService
                .getAnalysisHistory(user, PageRequest.of(page, size))
                .map(AnalysisResponse::from);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
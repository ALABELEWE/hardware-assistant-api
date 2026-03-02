package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.DataReadinessResult;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.service.impl.DataReadinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-readiness")
@RequiredArgsConstructor
public class DataReadinessController {

    private final DataReadinessService dataReadinessService;

    @GetMapping
    public ResponseEntity<ApiResponse<DataReadinessResult>> check(
            @AuthenticationPrincipal User user) {
        DataReadinessResult result = dataReadinessService.check(user);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
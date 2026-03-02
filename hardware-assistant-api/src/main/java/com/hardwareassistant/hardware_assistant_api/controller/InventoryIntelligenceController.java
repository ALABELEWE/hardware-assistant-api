package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.InventoryAlertResult;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.service.impl.InventoryIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory-alerts")
@RequiredArgsConstructor
public class InventoryIntelligenceController {

    private final InventoryIntelligenceService inventoryIntelligenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<InventoryAlertResult>> getAlerts(
            @AuthenticationPrincipal User user) {
        InventoryAlertResult result = inventoryIntelligenceService.analyze(user);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.AINarrativeResult;
import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.service.impl.AINarrativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/narrative")
@RequiredArgsConstructor
public class AINarrativeController {

    private final AINarrativeService aiNarrativeService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AINarrativeResult>> generate(
            @AuthenticationPrincipal User user) {
        AINarrativeResult result = aiNarrativeService.generate(user);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
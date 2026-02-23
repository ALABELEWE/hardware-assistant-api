package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.request.MerchantProfileRequest;
import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.MerchantProfileResponse;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> getProfile(
            @AuthenticationPrincipal User user) {
        MerchantProfileResponse response = MerchantProfileResponse
                .from(merchantService.getProfile(user));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> saveProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody MerchantProfileRequest request) {
        MerchantProfileResponse response = MerchantProfileResponse
                .from(merchantService.saveProfile(user, request));
        return ResponseEntity.ok(ApiResponse.success("Profile saved", response));
    }
}
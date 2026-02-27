package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.AnalysisResponse;
import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.exception.BusinessException;
import com.hardwareassistant.hardware_assistant_api.model.BusinessAnalysis;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.MerchantProfileRepository;
import com.hardwareassistant.hardware_assistant_api.repository.UserRepository;
import com.hardwareassistant.hardware_assistant_api.security.InputSanitizer;
import com.hardwareassistant.hardware_assistant_api.security.SanitizationResult;
import com.hardwareassistant.hardware_assistant_api.service.AnalysisService;
import com.hardwareassistant.hardware_assistant_api.service.impl.SecurityIncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final InputSanitizer inputSanitizer;
    private final SecurityIncidentService incidentService;

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

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<BusinessAnalysis>> generateAnalysis(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "false") boolean sendSms) {

        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        // ── Check if user is banned FIRST ────────────────────────────────────
        if (currentUser.isBanned()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.securityIncident(
                            "Your account has been suspended due to repeated policy violations. " +
                                    "Please contact support@hardwareai.org if you believe this is a mistake.",
                            "BANNED",
                            incidentService.countAttempts(currentUser)
                    ));
        }

        // ── Load profile for sanitisation ────────────────────────────────────
        MerchantProfile profile = merchantProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new BusinessException("Please complete your merchant profile first"));

        // ── Sanitise all profile fields before AI call ────────────────────────
        SanitizationResult sanitization = inputSanitizer.sanitizeMerchantProfile(
                profile.getBusinessName(),
                profile.getLocation(),
                profile.getProducts(),
                profile.getCustomerType(),
                currentUser
        );

        if (sanitization.isBlocked()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.securityIncident(
                            sanitization.message(),
                            "BLOCKED",
                            incidentService.countAttempts(currentUser)
                    ));
        }

        if (sanitization.isWarning()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.securityIncident(
                            sanitization.message(),
                            "WARNING",
                            incidentService.countAttempts(currentUser)
                    ));
        }


        BusinessAnalysis analysis = analysisService.generateAnalysis(currentUser, sendSms);
        return ResponseEntity.ok(ApiResponse.success("Analysis complete", analysis));
    }

}
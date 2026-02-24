package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    @Value("${app.payment.paystack-secret-key}")
    private String paystackSecretKey;

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<Map<String, String>>> initializePayment(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        String planName = request.getOrDefault("plan", "basic");
        int amount = planName.equals("premium") ? 500000 : 200000; // kobo: ₦5000 or ₦2000

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(paystackSecretKey);

            Map<String, Object> body = new HashMap<>();
            body.put("email", email);
            body.put("amount", amount);
            body.put("currency", "NGN");
            body.put("metadata", Map.of("plan_name", planName));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.paystack.co/transaction/initialize",
                    entity,
                    Map.class
            );

            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            String authorizationUrl = (String) data.get("authorization_url");
            String reference = (String) data.get("reference");

            Map<String, String> result = new HashMap<>();
            result.put("authorizationUrl", authorizationUrl);
            result.put("reference", reference);

            return ResponseEntity.ok(ApiResponse.success("Payment initialized", result));

        } catch (Exception e) {
            log.error("Payment initialization failed: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Payment initialization failed"));
        }
    }

    @GetMapping("/verify/{reference}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            @PathVariable String reference) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(paystackSecretKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.paystack.co/transaction/verify/" + reference,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            String status = (String) data.get("status");

            Map<String, Object> result = new HashMap<>();
            result.put("status", status);
            result.put("reference", reference);

            return ResponseEntity.ok(ApiResponse.success("Payment verified", result));

        } catch (Exception e) {
            log.error("Payment verification failed: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Payment verification failed"));
        }
    }
}
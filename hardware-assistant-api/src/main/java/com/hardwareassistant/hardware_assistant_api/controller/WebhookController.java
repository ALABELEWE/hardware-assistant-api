package com.hardwareassistant.hardware_assistant_api.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareassistant.hardware_assistant_api.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @PostMapping("/paystack")
    public ResponseEntity<Void> handlePaystackWebhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature) {

        if (!paymentService.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Paystack webhook signature");
            return ResponseEntity.badRequest().build();
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.get("event").asText();

            if ("charge.success".equals(eventType)) {
                JsonNode data = event.get("data");
                String reference = data.get("reference").asText();
                String email = data.get("customer").get("email").asText();
                String plan = data.path("metadata").path("plan_name").asText("basic");
                paymentService.handleSuccessfulPayment(reference, email, plan);
            }

        } catch (Exception e) {
            log.error("Error processing Paystack webhook: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}

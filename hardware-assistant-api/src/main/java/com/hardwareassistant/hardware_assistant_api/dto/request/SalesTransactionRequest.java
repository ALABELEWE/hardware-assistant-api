// ── SalesTransactionRequest.java ─────────────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class SalesTransactionRequest {

    // Either productId (existing) or productName (ad-hoc) must be provided
    private UUID productId;

    @Size(max = 200)
    private String productName;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal costPrice;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    @Size(max = 50)
    private String paymentMethod = "cash";

    @Size(max = 500)
    private String notes;
}
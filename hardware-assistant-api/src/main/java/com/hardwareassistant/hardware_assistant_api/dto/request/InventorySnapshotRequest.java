// ── InventorySnapshotRequest.java ─────────────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class InventorySnapshotRequest {

    private UUID productId;

    @Size(max = 200)
    private String productName;

    @NotNull(message = "Quantity counted is required")
    @DecimalMin(value = "0.0", message = "Quantity cannot be negative")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal quantityCounted;

    @DecimalMin(value = "0.0")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal unitCost;

    @NotNull(message = "Snapshot date is required")
    @PastOrPresent(message = "Snapshot date cannot be in the future")
    private LocalDate snapshotDate;

    @Size(max = 500)
    private String notes;
}



















































































































































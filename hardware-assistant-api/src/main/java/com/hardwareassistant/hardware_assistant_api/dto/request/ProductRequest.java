// ── ProductRequest.java ───────────────────────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must be under 200 characters")
    private String name;

    @Size(max = 100)
    private String category;

    @Size(max = 50)
    private String unit = "piece";

    @DecimalMin(value = "0.0", message = "Cost price cannot be negative")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal costPrice;

    @DecimalMin(value = "0.0", message = "Selling price cannot be negative")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.0", message = "Current stock cannot be negative")
    private BigDecimal currentStock = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Reorder level cannot be negative")
    private BigDecimal reorderLevel = BigDecimal.ZERO;
}
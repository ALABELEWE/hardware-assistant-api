// ── ExpenseRequest.java ───────────────────────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseRequest {

    @NotBlank(message = "Category is required")
    @Size(max = 100)
    private String category;

    @Size(max = 300)
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal amount;

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate;

    @Size(max = 50)
    private String paymentMethod = "cash";

    @Size(max = 500)
    private String notes;
}
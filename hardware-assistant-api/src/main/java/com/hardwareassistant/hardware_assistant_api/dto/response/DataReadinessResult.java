package com.hardwareassistant.hardware_assistant_api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataReadinessResult {

    private boolean hasData;           // true if merchant has any sales logged
    private int     salesThisMonth;
    private int     salesLastMonth;
    private int     totalProducts;
    private boolean hasExpenses;
    private String  readinessLevel;    // "NONE" | "PARTIAL" | "READY"
    private String  message;           // human-readable guidance
}
package com.hardwareassistant.hardware_assistant_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class InventoryAlertResult {

    private int totalAlerts;
    private int criticalCount;  // low stock
    private int warningCount;   // overstock
    private int infoCount;      // dead stock

    private List<InventoryAlert> alerts;

    @Data
    @Builder
    public static class InventoryAlert {
        private UUID productId;
        private String     productName;
        private String     category;
        private String     type;        // "LOW_STOCK" | "OVERSTOCK" | "DEAD_STOCK"
        private String     severity;    // "CRITICAL" | "WARNING" | "INFO"
        private String     message;     // human-readable alert
        private String     action;      // recommended action
        private BigDecimal currentStock;
        private BigDecimal reorderLevel;
        private BigDecimal avgMonthlySales; // null for dead stock
    }
}
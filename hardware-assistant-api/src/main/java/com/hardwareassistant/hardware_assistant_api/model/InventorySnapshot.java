package com.hardwareassistant.hardware_assistant_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_profile_id", nullable = false)
    private MerchantProfile merchantProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "quantity_counted", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityCounted;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    // Computed by DB: quantity_counted * unit_cost — read-only
    @Column(name = "total_value", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalValue;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (productName == null && product != null) {
            productName = product.getName();
        }
        if (unitCost == null && product != null && product.getCostPrice() != null) {
            unitCost = product.getCostPrice();
        }
    }
}
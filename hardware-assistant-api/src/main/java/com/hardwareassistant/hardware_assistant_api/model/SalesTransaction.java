package com.hardwareassistant.hardware_assistant_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_profile_id", nullable = false)
    private MerchantProfile merchantProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    // Computed by DB: quantity * unit_price — read-only in Java
    @Column(name = "total_amount", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalAmount;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "payment_method", length = 50)
    @Builder.Default
    private String paymentMethod = "cash";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 20)
    @Builder.Default
    private String source = "manual";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // Denormalise product name from linked product if not set
        if (productName == null && product != null) {
            productName = product.getName();
        }
        // Copy cost price from product at time of sale
        if (costPrice == null && product != null && product.getCostPrice() != null) {
            costPrice = product.getCostPrice();
        }
    }
}
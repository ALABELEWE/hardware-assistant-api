package com.hardwareassistant.hardware_assistant_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String planName;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "subscription_status")
    private SubscriptionStatus status;

    private String paymentProviderId;
    private LocalDateTime renewalDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum SubscriptionStatus { ACTIVE, INACTIVE, CANCELLED, EXPIRED }
}

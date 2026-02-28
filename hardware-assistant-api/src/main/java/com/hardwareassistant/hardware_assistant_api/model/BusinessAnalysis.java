package com.hardwareassistant.hardware_assistant_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "business_analyses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_profile_id", nullable = false)
    private MerchantProfile merchantProfile;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String aiResponseJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

package com.hardwareassistant.hardware_assistant_api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_incidents",
       indexes = {
           @Index(name = "idx_incident_user",      columnList = "user_id"),
           @Index(name = "idx_incident_created_at", columnList = "created_at"),
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SecurityIncident {

    public enum Severity { WARNING, BLOCKED, BANNED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The user who triggered the incident (nullable — could be unauthenticated)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_email")
    private String userEmail;

    // Which field contained the malicious input
    @Column(name = "field_name", nullable = false)
    private String fieldName;

    // First 300 chars of the offending input (enough for analysis, not too much storage)
    @Column(name = "flagged_input", length = 300)
    private String flaggedInput;

    // Which injection pattern matched
    @Column(name = "matched_pattern", length = 200)
    private String matchedPattern;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    // How many total attempts this user has made (denormalised for fast lookup)
    @Column(name = "user_attempt_count")
    private int userAttemptCount;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
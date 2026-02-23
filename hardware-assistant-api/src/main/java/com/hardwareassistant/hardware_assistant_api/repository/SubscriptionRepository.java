package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUserId(UUID userId);
    Optional<Subscription> findByPaymentProviderId(String paymentProviderId);
}

package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    @Query("SELECT COUNT(a) FROM AiUsage a WHERE a.merchant.id = :merchantId " +
           "AND a.createdAt >= :from AND a.createdAt <= :to")
    long countByMerchantIdAndPeriod(UUID merchantId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(a.totalTokens) FROM AiUsage a WHERE a.merchant.id = :merchantId " +
           "AND a.createdAt >= :from AND a.createdAt <= :to")
    Long sumTokensByMerchantIdAndPeriod(UUID merchantId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(a.estimatedCost) FROM AiUsage a WHERE a.createdAt >= :from")
    java.math.BigDecimal totalPlatformCostSince(LocalDateTime from);

    List<AiUsage> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    @Query("SELECT a.merchant.id, a.merchant.email, SUM(a.totalTokens), SUM(a.estimatedCost) " +
           "FROM AiUsage a GROUP BY a.merchant.id, a.merchant.email " +
           "ORDER BY SUM(a.estimatedCost) DESC")
    List<Object[]> getPerUserUsageSummary();
}
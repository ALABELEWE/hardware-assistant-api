package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    @Query("SELECT COUNT(a) FROM AiUsage a WHERE a.merchant.id = :merchantId " +
            "AND a.createdAt >= :from AND a.createdAt <= :to")
    long countByMerchantIdAndPeriod(@Param("merchantId") UUID merchantId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    @Query("SELECT SUM(a.totalTokens) FROM AiUsage a WHERE a.merchant.id = :merchantId " +
            "AND a.createdAt >= :from AND a.createdAt <= :to")
    Long sumTokensByMerchantIdAndPeriod(@Param("merchantId") UUID merchantId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("SELECT SUM(a.estimatedCost) FROM AiUsage a WHERE a.createdAt >= :from")
    BigDecimal totalPlatformCostSince(@Param("from") LocalDateTime from);

    // ── New queries for monthly trend ───────────────────────────────────

    @Query("SELECT SUM(a.totalTokens) FROM AiUsage a WHERE a.createdAt >= :from")
    Long sumTokensByPeriod(@Param("from") LocalDateTime from);

    @Query("SELECT COUNT(a) FROM AiUsage a WHERE a.createdAt >= :from")
    Long countAnalysesSince(@Param("from") LocalDateTime from);

    @Query("SELECT SUM(a.estimatedCost) FROM AiUsage a " +
            "WHERE a.createdAt >= :from AND a.createdAt < :to")
    BigDecimal totalPlatformCostBetween(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("SELECT SUM(a.totalTokens) FROM AiUsage a " +
            "WHERE a.createdAt >= :from AND a.createdAt < :to")
    Long sumTokensBetween(@Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(a) FROM AiUsage a " +
            "WHERE a.createdAt >= :from AND a.createdAt < :to")
    Long countAnalysesBetween(@Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    // ── Per-user summary (added analyses count) ─────────────────────────

    @Query("SELECT a.merchant.id, a.merchant.email, SUM(a.totalTokens), " +
            "SUM(a.estimatedCost), COUNT(a) " +
            "FROM AiUsage a GROUP BY a.merchant.id, a.merchant.email " +
            "ORDER BY SUM(a.estimatedCost) DESC")
    List<Object[]> getPerUserUsageSummary();

    List<AiUsage> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
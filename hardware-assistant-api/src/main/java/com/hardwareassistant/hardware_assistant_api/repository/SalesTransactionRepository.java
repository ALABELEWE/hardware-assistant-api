// ── SalesTransactionRepository.java ──────────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.SalesTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, UUID> {

    Page<SalesTransaction> findByMerchantProfileOrderByTransactionDateDescCreatedAtDesc(
            MerchantProfile profile, Pageable pageable);

    List<SalesTransaction> findByMerchantProfileAndTransactionDateBetweenOrderByTransactionDateDesc(
            MerchantProfile profile, LocalDate from, LocalDate to);

    Optional<SalesTransaction> findByIdAndMerchantProfile(UUID id, MerchantProfile profile);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SalesTransaction s " +
           "WHERE s.merchantProfile = :profile AND s.transactionDate BETWEEN :from AND :to")
    BigDecimal sumRevenueByPeriod(@Param("profile") MerchantProfile profile,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    @Query("SELECT COUNT(s) FROM SalesTransaction s " +
           "WHERE s.merchantProfile = :profile AND s.transactionDate BETWEEN :from AND :to")
    long countByPeriod(@Param("profile") MerchantProfile profile,
                       @Param("from") LocalDate from,
                       @Param("to") LocalDate to);
}

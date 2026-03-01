// ── ExpenseRecordRepository.java ──────────────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.ExpenseRecord;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
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
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecord, UUID> {

    Page<ExpenseRecord> findByMerchantProfileOrderByExpenseDateDescCreatedAtDesc(
            MerchantProfile profile, Pageable pageable);

    List<ExpenseRecord> findByMerchantProfileAndExpenseDateBetweenOrderByExpenseDateDesc(
            MerchantProfile profile, LocalDate from, LocalDate to);

    Optional<ExpenseRecord> findByIdAndMerchantProfile(UUID id, MerchantProfile profile);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseRecord e " +
           "WHERE e.merchantProfile = :profile AND e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumExpensesByPeriod(@Param("profile") MerchantProfile profile,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) FROM ExpenseRecord e " +
           "WHERE e.merchantProfile = :profile AND e.expenseDate BETWEEN :from AND :to " +
           "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumByCategory(@Param("profile") MerchantProfile profile,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);
}

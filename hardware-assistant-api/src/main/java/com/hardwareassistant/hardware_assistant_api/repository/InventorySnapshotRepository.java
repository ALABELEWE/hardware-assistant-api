// ── InventorySnapshotRepository.java ─────────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.InventorySnapshot;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, UUID> {

    Page<InventorySnapshot> findByMerchantProfileOrderBySnapshotDateDescCreatedAtDesc(
            MerchantProfile profile, Pageable pageable);

    Optional<InventorySnapshot> findByIdAndMerchantProfile(UUID id, MerchantProfile profile);

    // Latest snapshot per product (for current inventory state)
    @Query("SELECT s FROM InventorySnapshot s WHERE s.merchantProfile = :profile " +
           "AND s.snapshotDate = (SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2 " +
           "WHERE s2.merchantProfile = :profile AND s2.product = s.product)")
    List<InventorySnapshot> findLatestPerProduct(@Param("profile") MerchantProfile profile);

    List<InventorySnapshot> findByMerchantProfileAndSnapshotDateBetween(
            MerchantProfile profile, LocalDate from, LocalDate to);
}



















































































































































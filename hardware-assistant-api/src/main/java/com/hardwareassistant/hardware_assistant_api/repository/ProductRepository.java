// ── ProductRepository.java ────────────────────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByMerchantProfileAndActiveOrderByNameAsc(MerchantProfile profile, boolean active);

    List<Product> findByMerchantProfileOrderByNameAsc(MerchantProfile profile);

    Optional<Product> findByIdAndMerchantProfile(UUID id, MerchantProfile profile);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.merchantProfile = :profile AND p.active = true ORDER BY p.category")
    List<String> findCategoriesByMerchantProfile(MerchantProfile profile);

    boolean existsByNameIgnoreCaseAndMerchantProfile(String name, MerchantProfile profile);
}
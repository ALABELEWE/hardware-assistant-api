package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.BusinessAnalysis;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BusinessAnalysisRepository extends JpaRepository<BusinessAnalysis, UUID> {
    Page<BusinessAnalysis> findByMerchantProfileOrderByCreatedAtDesc(MerchantProfile profile, Pageable pageable);
    long countByMerchantProfile(MerchantProfile profile);
}

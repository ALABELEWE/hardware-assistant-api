package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SmsLogRepository extends JpaRepository<SmsLog, UUID> {
    List<SmsLog> findByMerchantProfileIdOrderBySentAtDesc(UUID merchantProfileId);
}
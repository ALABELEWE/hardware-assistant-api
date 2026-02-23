package com.hardwareassistant.hardware_assistant_api.service;

import com.hardwareassistant.hardware_assistant_api.model.BusinessAnalysis;
import com.hardwareassistant.hardware_assistant_api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnalysisService {
    BusinessAnalysis generateAnalysis(User user, boolean sendSms);

    Page<BusinessAnalysis> getAnalysisHistory(User user, Pageable pageable);
}

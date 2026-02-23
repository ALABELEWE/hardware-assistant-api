package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.model.Subscription;
import com.hardwareassistant.hardware_assistant_api.repository.BusinessAnalysisRepository;
import com.hardwareassistant.hardware_assistant_api.repository.MerchantProfileRepository;
import com.hardwareassistant.hardware_assistant_api.repository.SmsLogRepository;
import com.hardwareassistant.hardware_assistant_api.repository.SubscriptionRepository;
import com.hardwareassistant.hardware_assistant_api.repository.UserRepository;
import com.hardwareassistant.hardware_assistant_api.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final BusinessAnalysisRepository analysisRepository;
    private final SmsLogRepository smsLogRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalMerchants = merchantProfileRepository.count();
        long totalAnalyses = analysisRepository.count();
        long totalSmsSent = smsLogRepository.count();
        long activeSubscriptions = subscriptionRepository.findAll()
                .stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.ACTIVE)
                .count();

        log.debug("Admin dashboard stats fetched: users={}, merchants={}, analyses={}",
                totalUsers, totalMerchants, totalAnalyses);

        return Map.of(
                "totalUsers", totalUsers,
                "totalMerchants", totalMerchants,
                "totalAnalyses", totalAnalyses,
                "totalSmsSent", totalSmsSent,
                "activeSubscriptions", activeSubscriptions
        );
    }
}
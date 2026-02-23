package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.dto.request.MerchantProfileRequest;
import com.hardwareassistant.hardware_assistant_api.exception.ResourceNotFoundException;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.MerchantProfileRepository;
import com.hardwareassistant.hardware_assistant_api.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantServiceImpl implements MerchantService {
    private final MerchantProfileRepository merchantProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public MerchantProfile getProfile(User user) {
            return merchantProfileRepository.findByUser(user)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Profile not found. Please complete your business profile first."));
    }

    @Override
    @Transactional
    public MerchantProfile saveProfile(User user, MerchantProfileRequest request) {
        MerchantProfile profile = merchantProfileRepository.findByUser(user)
                .orElse(MerchantProfile.builder().user(user).build());

        profile.setBusinessName(request.getBusinessName());
        profile.setLocation(request.getLocation());
        profile.setCustomerType(request.getCustomerType());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setProducts(request.getProducts());
        profile.setPriceRange(request.getPriceRange());

        MerchantProfile saved = merchantProfileRepository.save(profile);
        log.info(" Profile saved for user: {}", user.getEmail());
        return saved;
    }
}

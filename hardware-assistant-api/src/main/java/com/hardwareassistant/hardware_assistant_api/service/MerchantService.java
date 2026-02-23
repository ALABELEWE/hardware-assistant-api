package com.hardwareassistant.hardware_assistant_api.service;

import com.hardwareassistant.hardware_assistant_api.dto.request.MerchantProfileRequest;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.User;

public interface MerchantService {
    MerchantProfile getProfile(User user);
    MerchantProfile saveProfile(User user, MerchantProfileRequest request);
}

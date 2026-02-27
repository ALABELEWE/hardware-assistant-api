package com.hardwareassistant.hardware_assistant_api.service;

import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.User;

import java.util.UUID;

public interface SmsService {
    void sendSms(User merchant, String message, UUID analysisId);
}

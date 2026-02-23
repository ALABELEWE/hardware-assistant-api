package com.hardwareassistant.hardware_assistant_api.service;

import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;

public interface SmsService {

    void sendSms(MerchantProfile profile, String message);
}

package com.hardwareassistant.hardware_assistant_api.dto.response;

import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MerchantProfileResponse {
    private UUID id;
    private String businessName;
    private String location;
    private String customerType;
    private String phoneNumber;
    private String products;
    private String priceRange;
    private LocalDateTime createdAt;

    public static MerchantProfileResponse from(MerchantProfile profile) {
        MerchantProfileResponse dto = new MerchantProfileResponse();
        dto.setId(profile.getId());
        dto.setBusinessName(profile.getBusinessName());
        dto.setLocation(profile.getLocation());
        dto.setCustomerType(profile.getCustomerType());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setProducts(profile.getProducts());
        dto.setPriceRange(profile.getPriceRange());
        dto.setCreatedAt(profile.getCreatedAt());
        return dto;
    }
}
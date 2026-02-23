package com.hardwareassistant.hardware_assistant_api.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantProfileRequest {
    @NotBlank private String businessName;
    private String location;
    private String customerType;
    private String phoneNumber;
    private String products;
    private String priceRange;
}
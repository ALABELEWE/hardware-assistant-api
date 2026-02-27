package com.hardwareassistant.hardware_assistant_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantProfileRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
    private String businessName;

    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @Size(max = 100, message = "Customer type must not exceed 100 characters")
    private String customerType;

    @Pattern(
            regexp = "^(\\+?[0-9\\s\\-().]{7,20})?$",
            message = "Please enter a valid phone number (e.g. +2348012345678)"
    )
    private String phoneNumber;

    @Size(max = 500, message = "Products description must not exceed 500 characters")
    private String products;

    @Size(max = 100, message = "Price range must not exceed 100 characters")
    private String priceRange;
}
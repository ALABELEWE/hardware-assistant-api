package com.hardwareassistant.hardware_assistant_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ResendVerificationRequest{
    @NotBlank @Email String email;

}
package com.hardwareassistant.hardware_assistant_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Map<String, String> fieldErrors;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }

    public static <T> ApiResponse<T> validationError(String message, Map<String, String> fieldErrors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setFieldErrors(fieldErrors);
        return response;
    }


    public static <T> ApiResponse<T> securityIncident(
            String message,
            String incidentType,   // "WARNING" or "BLOCKED"
            long attemptCount
    ) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("incidentType", incidentType);
        meta.put("attemptCount", String.valueOf(attemptCount));
        response.setFieldErrors(meta);
        return response;
    }

}
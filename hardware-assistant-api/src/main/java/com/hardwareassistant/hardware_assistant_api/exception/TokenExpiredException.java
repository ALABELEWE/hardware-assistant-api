package com.hardwareassistant.hardware_assistant_api.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) { super(message); }
}
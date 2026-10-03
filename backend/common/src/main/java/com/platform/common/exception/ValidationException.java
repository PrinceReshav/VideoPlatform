package com.platform.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationException extends BusinessException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String code, String message, Map<String, String> fieldErrors) {
        super(code, HttpStatus.BAD_REQUEST, message);
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        this("VALIDATION_FAILED", message, fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
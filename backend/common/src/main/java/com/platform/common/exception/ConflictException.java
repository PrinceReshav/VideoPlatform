package com.platform.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    public ConflictException(String code, String message) {
        super(code, HttpStatus.CONFLICT, message);
    }

    public ConflictException(String code, String message, Object... args) {
        super(code, HttpStatus.CONFLICT, message, args);
    }
}
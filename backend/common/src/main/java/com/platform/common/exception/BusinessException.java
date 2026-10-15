package com.platform.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;
    private final Object[] args;

    public BusinessException(String code, String message) {
        this(code, defaultHttpStatus(code), message);
    }

    private static HttpStatus defaultHttpStatus(String code) {
        if ("FORBIDDEN".equals(code) || "AUTH_ACCESS_DENIED".equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if ("NOT_FOUND".equals(code) || "RESOURCE_NOT_FOUND".equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.args = new Object[0];
    }

    public BusinessException(String code, HttpStatus httpStatus, String message, Object... args) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.args = args;
    }

    public BusinessException(String code, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
        this.args = new Object[0];
    }

    public BusinessException(String code, HttpStatus httpStatus, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
        this.args = args;
    }
}
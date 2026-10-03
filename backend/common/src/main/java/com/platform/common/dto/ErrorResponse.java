package com.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
    private String correlationId;
    private Map<String, String> validationErrors;

    public static ErrorResponse of(int status, String code, String message, String path, String correlationId) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .code(code)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .build();
    }

    public static ErrorResponse of(int status, String code, String message, String path, String correlationId, Map<String, String> validationErrors) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .code(code)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .validationErrors(validationErrors)
                .build();
    }
}
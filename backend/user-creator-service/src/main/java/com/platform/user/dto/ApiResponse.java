package com.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper (Feature 19: Creator Verification)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** Indicates if the request was successful */
    private boolean success;

    /** The data payload */
    private T data;

    /** Error message if the request failed */
    private String error;

    /** Timestamp of the response */
    private long timestamp;

    /**
     * Creates a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Creates a successful response with no data
     */
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Creates an error response
     */
    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
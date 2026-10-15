package com.platform.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSessionRequest {

    private List<PartInfo> parts;

    private String checksum; // Final SHA-256 for verification

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartInfo {
        @NotNull
        @Positive
        private Integer partNumber;

        @NotBlank
        private String etag;
    }
}
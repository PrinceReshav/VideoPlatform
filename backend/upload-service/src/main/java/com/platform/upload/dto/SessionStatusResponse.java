package com.platform.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusResponse {

    private UUID id;
    private UUID userId;
    private UUID videoId;
    private String fileName;
    private String contentType;
    private Long totalSize;
    private Long chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Double progressPercent;
    private String status;
    private String minioUploadId;
    private String objectKey;
    private String bucket;
    private Instant expiresAt;
    private Instant completedAt;
    private String errorMessage;
    private List<Integer> uploadedPartNumbers;
}
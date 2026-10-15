package com.platform.video.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for requesting offline downloads (Feature 34: Offline Downloads with DRM)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineDownloadRequest {

    /** The video to download for offline viewing */
    @NotNull(message = "Video ID is required")
    private UUID videoId;

    /** Optional expiration time (if not provided, uses default based on license) */
    private Instant expiresAt;
}
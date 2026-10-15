package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for video share analytics requests (Feature 13: Privacy & Sharing Controls)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoShareAnalyticsRequest {

    /** The share link being analyzed */
    @NotNull
    private UUID shareLinkId;

    /** The video being shared (denormalized for easier querying) */
    @NotNull
    private UUID videoId;

    /** The user who created the share link */
    @NotNull
    private UUID createdBy;

    /** IP address of the viewer */
    @Size(max = 45)
    private String ipAddress;

    /** User agent of the viewer */
    @Size(max = 255)
    private String userAgent;

    /** Referrer URL (where the viewer came from) */
    @Size(max = 500)
    private String referrerUrl;

    /** Country code of the viewer (based on IP) */
    @Size(max = 2)
    private String countryCode;

    /** Whether the access was successful */
    private boolean success = true;

    /** Reason for failure if access was not successful */
    @Size(max = 255)
    private String failureReason;
}
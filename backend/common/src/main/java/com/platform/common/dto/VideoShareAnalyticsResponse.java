package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for video share analytics responses (Feature 13: Privacy & Sharing Controls)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoShareAnalyticsResponse {

    /** The unique identifier of the video share analytics record */
    private UUID id;

    /** The share link being analyzed */
    private UUID shareLinkId;

    /** The video being shared (denormalized for easier querying) */
    private UUID videoId;

    /** The user who created the share link */
    private UUID createdBy;

    /** Timestamp when the share link was accessed */
    private Instant accessedAt;

    /** IP address of the viewer */
    private String ipAddress;

    /** User agent of the viewer */
    private String userAgent;

    /** Referrer URL (where the viewer came from) */
    private String referrerUrl;

    /** Country code of the viewer (based on IP) */
    private String countryCode;

    /** Whether the access was successful */
    private boolean success;

    /** Reason for failure if access was not successful */
    private String failureReason;
}
package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for strike responses (Feature 18: Reporting/Strikes/Trust System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrikeResponse {

    /** The unique identifier of the strike */
    private UUID id;

    /** The target receiving the strike (user ID, channel ID, etc.) */
    private UUID targetId;

    /** The type of target receiving the strike */
    private String targetType; // USER, CHANNEL, CREATOR_PROFILE

    /** The reason for the strike */
    private String reason; // COPYRIGHT_VIOLATION, COMMUNITY_GUIDELINES, TERMS_OF_SERVICE, etc.

    /** Details about the violation */
    private String details;

    /** Whether the strike is currently active (not expired) */
    private boolean isActive;

    /** Timestamp when the strike was issued */
    private Instant issuedAt;

    /** Timestamp when the strike expires (null = permanent) */
    private Instant expiresAt;

    /** Moderator who issued the strike */
    private UUID issuedBy;

    /** Reference to the report that led to this strike (if applicable) */
    private UUID reportId;

    /** Timestamp when the strike was created */
    private Instant createdAt;

    /** Timestamp when the strike was last updated */
    private Instant updatedAt;
}
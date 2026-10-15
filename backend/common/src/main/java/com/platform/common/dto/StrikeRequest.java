package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for strike requests (Feature 18: Reporting/Strikes/Trust System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrikeRequest {

    /** The target receiving the strike (user ID, channel ID, etc.) */
    @NotNull
    private UUID targetId;

    /** The type of target receiving the strike */
    @NotBlank
    @Size(max = 50)
    private String targetType; // USER, CHANNEL, CREATOR_PROFILE

    /** The reason for the strike */
    @NotBlank
    @Size(max = 100)
    private String reason; // COPYRIGHT_VIOLATION, COMMUNITY_GUIDELINES, TERMS_OF_SERVICE, etc.

    /** Details about the violation */
    @Size(max = 2000)
    private String details;

    /** Timestamp when the strike expires (null = permanent) */
    private Instant expiresAt;
}
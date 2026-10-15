package com.platform.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for verification requests (Feature 19: Creator Verification)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequestDTO {

    /** Type of verification requested: CHANNEL, CREATOR_PROFILE, BOTH */
    @NotBlank
    private String requestType; // CHANNEL, CREATOR_PROFILE, BOTH

    /** Verification tier being requested: BASIC, VERIFIED, OFFICIAL */
    private String requestedTier; // BASIC, VERIFIED, OFFICIAL

    /** Reason for requesting verification */
    @Size(max = 1000)
    private String requestReason;

    /** Additional information or documentation URLs (JSON format) */
    private String additionalInfo;
}
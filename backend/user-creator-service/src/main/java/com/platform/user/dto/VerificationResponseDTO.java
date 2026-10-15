package com.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for verification responses (Feature 19: Creator Verification)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDTO {

    /** The unique identifier of the verification request */
    private UUID id;

    /** The user making the verification request */
    private UUID userId;

    /** Type of verification requested: CHANNEL, CREATOR_PROFILE, BOTH */
    private String requestType; // CHANNEL, CREATOR_PROFILE, BOTH

    /** Current status of the verification request */
    private String status; // PENDING, APPROVED, REJECTED

    /** Verification tier being requested: BASIC, VERIFIED, OFFICIAL */
    private String requestedTier; // BASIC, VERIFIED, OFFICIAL

    /** Reason for requesting verification */
    private String requestReason;

    /** Additional information or documentation URLs (JSON format) */
    private String additionalInfo;

    /** Admin/moderator who reviewed the request */
    private UUID reviewedBy;

    /** Timestamp when the request was reviewed */
    private Instant reviewedAt;

    /** Feedback or reason for rejection */
    private String reviewFeedback;

    /** Whether the request has been processed (approved or rejected) */
    private boolean processed;

    /** Timestamp when the request was created */
    private Instant createdAt;

    /** Timestamp when the request was last updated */
    private Instant updatedAt;
}
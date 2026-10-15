package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for copyright dispute responses (Feature 3: Copyright Detection & Claims System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyrightDisputeResponse {

    /** The unique identifier of the copyright dispute */
    private UUID id;

    /** The copyright claim being disputed */
    private UUID copyrightClaimId;

    /** The user filing the dispute (usually the video creator/uploader) */
    private UUID disputantId;

    /** The reason for the dispute */
    private String disputeReason;

    /** Explanation or evidence supporting the dispute */
    private String explanation;

    /** Type of dispute */
    private String disputeType; // FAIR_USE, NOT_INFRINGING, LICENSED, PUBLIC_DOMAIN, etc.

    /** Status of the dispute */
    private String status; // PENDING, UNDER_REVIEW, ACCEPTED, REJECTED, WITHDRAWN

    /** Action taken on the copyright claim due to the dispute */
    private String actionTaken; // NONE, CLAIM_RELEASED, CLAIM_UPHELD, SETTLED

    /** Whether the dispute has been legally reviewed */
    private boolean legallyReviewed;

    /** Evidence provided by the disputant */
    private String evidence;

    /** Response from the claimant */
    private String claimantResponse;

    /** Timestamp when the dispute was filed */
    private Instant filedAt;

    /** Timestamp when the dispute was last updated */
    private Instant updatedAt;

    /** Timestamp when the dispute expires (if applicable) */
    private Instant expiresAt;
}
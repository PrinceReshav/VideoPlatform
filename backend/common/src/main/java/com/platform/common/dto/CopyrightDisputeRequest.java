package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for copyright dispute requests (Feature 3: Copyright Detection & Claims System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyrightDisputeRequest {

    /** The copyright claim being disputed */
    @NotNull
    private UUID copyrightClaimId;

    /** The user filing the dispute (usually the video creator/uploader) */
    @NotNull
    private UUID disputantId;

    /** The reason for the dispute */
    @Size(max = 500)
    private String disputeReason;

    /** Explanation or evidence supporting the dispute */
    private String explanation;

    /** Type of dispute */
    @Size(max = 50)
    private String disputeType; // FAIR_USE, NOT_INFRINGING, LICENSED, PUBLIC_DOMAIN, etc.

    /** Evidence provided by the disputant */
    private String evidence;

    /** Timestamp when the dispute expires (if applicable) */
    private Instant expiresAt;
}
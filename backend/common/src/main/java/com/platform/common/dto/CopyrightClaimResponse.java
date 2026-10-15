package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for copyright claim responses (Feature 3: Copyright Detection & Claims System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyrightClaimResponse {

    /** The unique identifier of the copyright claim */
    private UUID id;

    /** The video that allegedly infringes copyright */
    private UUID videoId;

    /** The user who filed the copyright claim */
    private UUID claimantId;

    /** The copyright holder or rights owner */
    private UUID rightsOwnerId;

    /** Description of the copyrighted work */
    private String copyrightedWorkDescription;

    /** Registration number or identifier for the copyrighted work */
    private String copyrightRegistrationNumber;

    /** URL or reference to the original copyrighted work */
    private String originalWorkUrl;

    /** Timestamp when the alleged infringement occurs in the video (in seconds) */
    private Integer infringementTimestamp;

    /** Duration of the alleged infringement in the video (in seconds) */
    private Integer infringementDuration;

    /** Type of copyright infringement */
    private String infringementType; // VIDEO, AUDIO, IMAGE, TEXT, etc.

    /** Status of the copyright claim */
    private String status; // PENDING, VALIDATED, REJECTED, WITHDRAWN, SETTLED

    /** Action taken on the video due to the claim */
    private String actionTaken; // NONE, MONETIZE, BLOCK, TRACK, TAKE_DOWN

    /** Whether the claim has been legally validated */
    private boolean legallyValidated;

    /** Evidence provided by the claimant */
    private String evidence;

    /** Response from the video uploader/creator */
    private String creatorResponse;

    /** Timestamp when the claim was filed */
    private Instant filedAt;

    /** Timestamp when the claim was last updated */
    private Instant updatedAt;

    /** Timestamp when the claim expires (if applicable) */
    private Instant expiresAt;
}
package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for copyright claim requests (Feature 3: Copyright Detection & Claims System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyrightClaimRequest {

    /** The video that allegedly infringes copyright */
    @NotNull
    private UUID videoId;

    /** The user who filed the copyright claim */
    @NotNull
    private UUID claimantId;

    /** The copyright holder or rights owner */
    @NotNull
    private UUID rightsOwnerId;

    /** Description of the copyrighted work */
    @Size(max = 500)
    private String copyrightedWorkDescription;

    /** Registration number or identifier for the copyrighted work */
    @Size(max = 100)
    private String copyrightRegistrationNumber;

    /** URL or reference to the original copyrighted work */
    @Size(max = 500)
    private String originalWorkUrl;

    /** Timestamp when the alleged infringement occurs in the video (in seconds) */
    @Min(0)
    private Integer infringementTimestamp;

    /** Duration of the alleged infringement in the video (in seconds) */
    @Min(0)
    private Integer infringementDuration;

    /** Type of copyright infringement */
    @Size(max = 50)
    private String infringementType; // VIDEO, AUDIO, IMAGE, TEXT, etc.

    /** Evidence provided by the claimant */
    private String evidence;

    /** Timestamp when the claim expires (if applicable) */
    private Instant expiresAt;
}
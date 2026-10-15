package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for video share link requests (Feature 13: Privacy & Sharing Controls)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoShareLinkRequest {

    /** The video being shared */
    @NotNull
    private UUID videoId;

    /** The user who created the share link */
    @NotNull
    private UUID createdBy;

    /** Description or note about this share link */
    @Size(max = 500)
    private String description;

    /** Maximum number of uses for this link (0 = unlimited) */
    @Min(0)
    private Integer maxUses;

    /** Whether this link is password protected */
    private boolean passwordProtected;

    /** Password for accessing the video via this link (hashed) */
    @Size(max = 255)
    private String passwordHash;

    /** Expiration date for this share link */
    private Instant expiresAt;
}
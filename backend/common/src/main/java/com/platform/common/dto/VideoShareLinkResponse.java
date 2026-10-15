package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for video share link responses (Feature 13: Privacy & Sharing Controls)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoShareLinkResponse {

    /** The unique identifier of the video share link */
    private UUID id;

    /** The video being shared */
    private UUID videoId;

    /** The user who created the share link */
    private UUID createdBy;

    /** The share token (unique identifier for the link) */
    private String shareToken;

    /** Description or note about this share link */
    private String description;

    /** Maximum number of uses for this link (0 = unlimited) */
    private Integer maxUses;

    /** Current number of times this link has been used */
    private Integer currentUses;

    /** Whether this link is password protected */
    private boolean passwordProtected;

    /** Password for accessing the video via this link (hashed) */
    private String passwordHash;

    /** Expiration date for this share link */
    private Instant expiresAt;

    /** Timestamp when the share link was created */
    private Instant createdAt;

    /** Timestamp when the share link was last used */
    private Instant lastUsedAt;
}
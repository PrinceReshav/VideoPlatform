package com.platform.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Video share link entity for Feature 13: Privacy & Sharing Controls
 * Represents a shareable link to a video with optional tracking and expiration
 */
@Entity
@Table(name = "video_share_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE video_share_links SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class VideoShareLink {

    @Id
    @GeneratedValue
    private UUID id;

    /** The video being shared */
    private UUID videoId;

    /** The user who created the share link */
    private UUID createdBy;

    /** The share token (unique identifier for the link) */
    @Column(length = 100, unique = true, nullable = false)
    private String shareToken;

    /** Description or note about this share link */
    @Column(length = 500)
    private String description;

    /** Maximum number of uses for this link (0 = unlimited) */
    private Integer maxUses;

    /** Current number of times this link has been used */
    private Integer currentUses = 0;

    /** Whether this link is password protected */
    private boolean passwordProtected;

    /** Password for accessing the video via this link (hashed) */
    @Column(length = 255)
    private String passwordHash;

    /** Expiration date for this share link */
    private Instant expiresAt;

    /** Timestamp when the share link was created */
    private Instant createdAt;

    /** Timestamp when the share link was last used */
    private Instant lastUsedAt;

    /** Soft delete flag */
    private boolean deleted = false;
}
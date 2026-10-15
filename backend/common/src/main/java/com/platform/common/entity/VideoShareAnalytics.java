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
 * Video share analytics entity for Feature 13: Privacy & Sharing Controls
 * Tracks analytics for video share links
 */
@Entity
@Table(name = "video_share_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE video_share_analytics SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class VideoShareAnalytics {

    @Id
    @GeneratedValue
    private UUID id;

    /** The share link being analyzed */
    private UUID shareLinkId;

    /** The video being shared (denormalized for easier querying) */
    private UUID videoId;

    /** The user who created the share link */
    private UUID createdBy;

    /** Timestamp when the share link was accessed */
    private Instant accessedAt;

    /** IP address of the viewer */
    @Column(length = 45)
    private String ipAddress;

    /** User agent of the viewer */
    @Column(length = 255)
    private String userAgent;

    /** Referrer URL (where the viewer came from) */
    @Column(length = 500)
    private String referrerUrl;

    /** Country code of the viewer (based on IP) */
    @Column(length = 2)
    private String countryCode;

    /** Whether the access was successful */
    private boolean success = true;

    /** Reason for failure if access was not successful */
    @Column(length = 255)
    private String failureReason;

    /** Soft delete flag */
    private boolean deleted = false;
}
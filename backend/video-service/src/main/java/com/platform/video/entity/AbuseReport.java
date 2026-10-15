
package com.platform.video.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for abuse reports (Feature 33: Anti-Bot/Anti-Abuse System).
 * Tracks reports of abusive content or behavior.
 */
@Entity
@Table(
        name = "abuse_reports",
        schema = "video_schema",
        indexes = {
                @Index(
                        name = "idx_abuse_reports_reporter",
                        columnList = "reporter_id"
                ),
                @Index(
                        name = "idx_abuse_reports_reported_user",
                        columnList = "reported_user_id"
                ),
                @Index(
                        name = "idx_abuse_reports_video",
                        columnList = "video_id"
                ),
                @Index(
                        name = "idx_abuse_reports_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_abuse_reports_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
public class AbuseReport extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user who reported the abuse. */
    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    /** The user being reported, if applicable. */
    @Column(name = "reported_user_id")
    private UUID reportedUserId;

    /** The video being reported, if applicable. */
    @Column(name = "video_id")
    private UUID videoId;

    /** The comment being reported, if applicable. */
    @Column(name = "comment_id")
    private UUID commentId;

    /** The live stream being reported, if applicable. */
    @Column(name = "live_stream_id")
    private UUID liveStreamId;

    /** The type of abuse being reported. */
    @Column(name = "abuse_type", nullable = false, length = 50)
    private String abuseType;

    /** Detailed description of the abuse. */
    @Column(name = "description", length = 5000)
    private String description;

    /** Current status of the abuse report. */
    @Column(name = "status", nullable = false, length = 20)
    private AbuseReportStatus status = AbuseReportStatus.PENDING;

    /** Moderator who reviewed the report, if applicable. */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    /** Notes from the moderator's review. */
    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;

    /** When the report was reviewed. */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public enum AbuseReportStatus {
        PENDING,
        REVIEWED,
        RESOLVED,
        DISMISSED
    }
}
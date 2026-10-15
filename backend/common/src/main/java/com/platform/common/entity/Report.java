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
 * Report entity for Feature 18: Reporting/Strikes/Trust System
 * Represents a user report against content, channels, or other users
 */
@Entity
@Table(name = "reports",
        indexes = {
                @Index(name = "idx_reports_reporter_created", columnList = "reporter_id, created_at"),
                @Index(name = "idx_reports_target_created", columnList = "target_id, target_type, created_at"),
                @Index(name = "idx_reports_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE reports SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Report {

    @Id
    @GeneratedValue
    private UUID id;

    /** The user who made the report */
    @Column(nullable = false)
    private UUID reporterId;

    /** The target being reported (video ID, channel ID, user ID, comment ID, etc.) */
    @Column(nullable = false)
    private UUID targetId;

    /** The type of target being reported */
    @Column(length = 50, nullable = false)
    private String targetType; // VIDEO, CHANNEL, USER, COMMENT, PLAYLIST, etc.

    /** The reason for the report */
    @Column(length = 100, nullable = false)
    private String reason; // HARASSMENT, HATE_SPEECH, VIOLENCE, MISINFORMATION, COPYRIGHT, PRIVACY, SPAM, etc.

    /** Detailed description of the issue */
    @Column(length = 2000)
    private String description;

    /** Additional evidence or context (JSON format) */
    @Column(columnDefinition = "jsonb")
    private String evidence;

    /** The current status of the report */
    @Column(length = 20, nullable = false)
    private String status; // PENDING, UNDER_REVIEW, APPROVED, REJECTED, RESOLVED

    /** Action taken based on the report */
    @Column(length = 50)
    private String actionTaken; // NONE, WARNING, STRIKE, CONTENT_REMOVED, ACCOUNT_SUSPENDED, TERMINATED

    /** Number of strikes assigned (if any) */
    private Integer strikesAssigned = 0;

    /** Moderator who reviewed the report */
    private UUID reviewedBy;

    /** Timestamp when the report was created */
    private Instant createdAt;

    /** Timestamp when the report was last updated */
    private Instant updatedAt;

    /** Timestamp when the report was reviewed */
    private Instant reviewedAt;

    /** Soft delete flag */
    private boolean deleted = false;
}
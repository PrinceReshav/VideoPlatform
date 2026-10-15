package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for report responses (Feature 18: Reporting/Strikes/Trust System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    /** The unique identifier of the report */
    private UUID id;

    /** The user who made the report */
    private UUID reporterId;

    /** The target being reported (video ID, channel ID, user ID, comment ID, etc.) */
    private UUID targetId;

    /** The type of target being reported */
    private String targetType; // VIDEO, CHANNEL, USER, COMMENT, PLAYLIST, etc.

    /** The reason for the report */
    private String reason; // HARASSMENT, HATE_SPEECH, VIOLENCE, MISINFORMATION, COPYRIGHT, PRIVACY, SPAM, etc.

    /** Detailed description of the issue */
    private String description;

    /** Additional evidence or context (JSON format) */
    private String evidence;

    /** The current status of the report */
    private String status; // PENDING, UNDER_REVIEW, APPROVED, REJECTED, RESOLVED

    /** Action taken based on the report */
    private String actionTaken; // NONE, WARNING, STRIKE, CONTENT_REMOVED, ACCOUNT_SUSPENDED, TERMINATED

    /** Number of strikes assigned (if any) */
    private Integer strikesAssigned;

    /** Moderator who reviewed the report */
    private UUID reviewedBy;

    /** Timestamp when the report was created */
    private Instant createdAt;

    /** Timestamp when the report was last updated */
    private Instant updatedAt;

    /** Timestamp when the report was reviewed */
    private Instant reviewedAt;
}
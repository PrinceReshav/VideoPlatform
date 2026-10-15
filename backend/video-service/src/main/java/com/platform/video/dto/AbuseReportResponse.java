package com.platform.video.dto;

import com.platform.video.entity.AbuseReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for abuse report responses (Feature 33: Anti-Bot/Anti-Abuse System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbuseReportResponse {

    /** The abuse report ID */
    private UUID id;

    /** The user who reported the abuse */
    private UUID reporterId;

    /** The user being reported (if applicable) */
    private UUID reportedUserId;

    /** The video being reported (if applicable) */
    private UUID videoId;

    /** The comment being reported (if applicable) */
    private UUID commentId;

    /** The live stream being reported (if applicable) */
    private UUID liveStreamId;

    /** The type of abuse being reported */
    private String abuseType;

    /** Detailed description of the abuse */
    private String description;

    /** Current status of the abuse report */
    private AbuseReport.AbuseReportStatus status;

    /** Moderator who reviewed the report (if applicable) */
    private UUID reviewedBy;

    /** Notes from the moderator's review */
    private String reviewNotes;

    /** When the report was reviewed */
    private Instant reviewedAt;

    /** When the abuse report was created */
    private Instant createdAt;

    /** When the abuse report was last updated */
    private Instant updatedAt;

    /**
     * Convert AbuseReport entity to AbuseReportResponse DTO
     */
    public static AbuseReportResponse fromEntity(AbuseReport abuseReport) {
        return AbuseReportResponse.builder()
                .id(abuseReport.getId())
                .reporterId(abuseReport.getReporterId())
                .reportedUserId(abuseReport.getReportedUserId())
                .videoId(abuseReport.getVideoId())
                .commentId(abuseReport.getCommentId())
                .liveStreamId(abuseReport.getLiveStreamId())
                .abuseType(abuseReport.getAbuseType())
                .description(abuseReport.getDescription())
                .status(abuseReport.getStatus())
                .reviewedBy(abuseReport.getReviewedBy())
                .reviewNotes(abuseReport.getReviewNotes())
                .reviewedAt(abuseReport.getReviewedAt())
                .createdAt(abuseReport.getCreatedAt())
                .updatedAt(abuseReport.getUpdatedAt())
                .build();
    }
}
package com.platform.video.repository;

import com.platform.video.entity.AbuseReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AbuseReport entities
 * (Feature 33: Anti-Bot/Anti-Abuse System)
 */
@Repository
public interface AbuseReportRepository
        extends JpaRepository<AbuseReport, UUID> {

    /** Find abuse reports by reporter ID */
    List<AbuseReport> findByReporterId(UUID reporterId);

    /** Find abuse reports by reported user ID */
    List<AbuseReport> findByReportedUserId(UUID reportedUserId);

    /** Find abuse reports by video ID */
    List<AbuseReport> findByVideoId(UUID videoId);

    /** Find abuse reports by comment ID */
    List<AbuseReport> findByCommentId(UUID commentId);

    /** Find abuse reports by live stream ID */
    List<AbuseReport> findByLiveStreamId(UUID liveStreamId);

    /** Find abuse reports by status */
    List<AbuseReport> findByStatus(
            AbuseReport.AbuseReportStatus status
    );

    /** Find abuse reports that need review (pending) */
    default List<AbuseReport> findPendingReports() {
        return findByStatus(AbuseReport.AbuseReportStatus.PENDING);
    }

    /** Find abuse reports created after a given time */
    List<AbuseReport> findByCreatedAtAfter(Instant createdAt);
}
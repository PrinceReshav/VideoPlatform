package com.platform.video.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.video.entity.AbuseReport;
import com.platform.video.repository.AbuseReportRepository;
import com.platform.video.dto.AbuseReportRequest;
import com.platform.video.dto.AbuseReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling abuse reports (Feature 33: Anti-Bot/Anti-Abuse System)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbuseReportService {

    private final AbuseReportRepository abuseReportRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Create a new abuse report
     */
    @Transactional
    public AbuseReportResponse createAbuseReport(AbuseReportRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        // Validate that at least one target is specified
        if (request.getReportedUserId() == null &&
            request.getVideoId() == null &&
            request.getCommentId() == null &&
            request.getLiveStreamId() == null) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                "At least one target (user, video, comment, or live stream) must be specified for the abuse report");
        }

        AbuseReport abuseReport = new AbuseReport();
        abuseReport.setReporterId(userId);
        abuseReport.setReportedUserId(request.getReportedUserId());
        abuseReport.setVideoId(request.getVideoId());
        abuseReport.setCommentId(request.getCommentId());
        abuseReport.setLiveStreamId(request.getLiveStreamId());
        abuseReport.setAbuseType(request.getAbuseType());
        abuseReport.setDescription(request.getDescription());
        abuseReport.setStatus(AbuseReport.AbuseReportStatus.PENDING);
        abuseReport.setCreatedAt(Instant.now());
        abuseReport.setUpdatedAt(Instant.now());

        AbuseReport savedReport = abuseReportRepository.save(abuseReport);
        log.info("Created abuse report {} by user {}", savedReport.getId(), userId);

        return AbuseReportResponse.fromEntity(savedReport);
    }

    /**
     * Get an abuse report by ID
     */
    @Transactional(readOnly = true)
    public AbuseReportResponse getAbuseReport(UUID id) {
        AbuseReport abuseReport = abuseReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Abuse report not found"));

        // Check if user has permission to view this abuse report
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!abuseReport.getReporterId().equals(userId) &&
            !isModerator(userId)) {
            // Only allow reporters and moderators to view abuse reports
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to view this abuse report");
        }

        return AbuseReportResponse.fromEntity(abuseReport);
    }

    /**
     * Get abuse reports by reporter ID
     */
    @Transactional(readOnly = true)
    public List<AbuseReportResponse> getAbuseReportsByReporter(UUID reporterId) {
        // Check if user is requesting their own reports
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!userId.equals(reporterId) && !isModerator(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You can only view your own abuse reports");
        }

        List<AbuseReport> reports = abuseReportRepository.findByReporterId(reporterId);
        return reports.stream()
                .map(AbuseReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get abuse reports that need review (for moderators)
     */
    @Transactional(readOnly = true)
    public List<AbuseReportResponse> getPendingAbuseReports() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!isModerator(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "Only moderators can view pending abuse reports");
        }

        List<AbuseReport> reports = abuseReportRepository.findPendingReports();
        return reports.stream()
                .map(AbuseReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Review and resolve an abuse report (moderator action)
     */
    @Transactional
    public AbuseReportResponse reviewAbuseReport(UUID id, String reviewNotes,
                                                 AbuseReport.AbuseReportStatus status) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!isModerator(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "Only moderators can review abuse reports");
        }

        AbuseReport abuseReport = abuseReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Abuse report not found"));

        // Validate status transition
        if (abuseReport.getStatus() != AbuseReport.AbuseReportStatus.PENDING) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                "Only pending abuse reports can be reviewed");
        }

        abuseReport.setReviewedBy(userId);
        abuseReport.setReviewNotes(reviewNotes);
        abuseReport.setStatus(status);
        abuseReport.setReviewedAt(Instant.now());
        abuseReport.setUpdatedAt(Instant.now());

        AbuseReport updatedReport = abuseReportRepository.save(abuseReport);
        log.info("Reviewed abuse report {} by moderator {}", updatedReport.getId(), userId);

        return AbuseReportResponse.fromEntity(updatedReport);
    }

    /**
     * Get abuse reports by video ID
     */
    @Transactional(readOnly = true)
    public List<AbuseReportResponse> getAbuseReportsByVideo(UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        // In a real implementation, you'd check if the user has permission to view reports on this video
        // For now, we'll allow authenticated users to see reports on videos

        List<AbuseReport> reports = abuseReportRepository.findByVideoId(videoId);
        return reports.stream()
                .map(AbuseReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Check if the current user is a moderator
     * In a real implementation, this would check user roles/permissions
     */
    private boolean isModerator(UUID userId) {
        // For now, we'll use a simple check - in reality, this would come from a user service or auth service
        // This is a placeholder implementation
        return false; // Assume no users are moderators for now
    }
}
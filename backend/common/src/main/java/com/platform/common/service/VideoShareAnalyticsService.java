package com.platform.common.service;

import com.platform.common.entity.VideoShareAnalytics;
import com.platform.common.repository.VideoShareAnalyticsRepository;
import com.platform.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling video share analytics (Feature 13: Privacy & Sharing Controls)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoShareAnalyticsService {

    private final VideoShareAnalyticsRepository videoShareAnalyticsRepository;

    /**
     * Record an access to a video share link
     */
    @Transactional
    public void recordShareAccess(
            UUID shareLinkId,
            UUID videoId,
            UUID createdBy,
            HttpServletRequest request,
            boolean success,
            String failureReason) {

        VideoShareAnalytics analytics = VideoShareAnalytics.builder()
                .shareLinkId(shareLinkId)
                .videoId(videoId)
                .createdBy(createdBy)
                .accessedAt(Instant.now())
                .ipAddress(request != null ? IpUtil.getClientIpAddress(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .referrerUrl(request != null ? request.getHeader("Referer") : null)
                .success(success)
                .failureReason(failureReason)
                .build();

        videoShareAnalyticsRepository.save(analytics);
        log.info("Recorded share analytics for link {}: success={}", shareLinkId, success);
    }

    /**
     * Get analytics for a share link
     */
    public List<VideoShareAnalytics> getAnalyticsByShareLinkId(UUID shareLinkId) {
        return videoShareAnalyticsRepository.findByShareLinkId(shareLinkId);
    }

    /**
     * Get analytics for a video
     */
    public List<VideoShareAnalytics> getAnalyticsByVideoId(UUID videoId) {
        return videoShareAnalyticsRepository.findByVideoId(videoId);
    }

    /**
     * Get analytics by a creator
     */
    public List<VideoShareAnalytics> getAnalyticsByCreatedBy(UUID createdBy) {
        return videoShareAnalyticsRepository.findByCreatedBy(createdBy);
    }

    /**
     * Get analytics by timestamp range
     */
    public List<VideoShareAnalytics> getAnalyticsByTimestampRange(Instant startTime, Instant endTime) {
        return videoShareAnalyticsRepository.findByAccessedAtBetween(startTime, endTime);
    }

    /**
     * Get analytics with filtering and pagination
     */
    public List<VideoShareAnalytics> getAnalyticsByCriteria(
            UUID shareLinkId,
            UUID videoId,
            UUID createdBy,
            Instant startTime,
            Instant endTime) {

        return videoShareAnalyticsRepository.findByCriteria(
                shareLinkId, videoId, createdBy, startTime, endTime,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
    }
}
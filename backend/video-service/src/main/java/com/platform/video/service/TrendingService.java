package com.platform.video.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.entity.Video;
import com.platform.video.entity.VideoMetrics;
import com.platform.video.repository.VideoMetricsRepository;
import com.platform.video.repository.VideoRepository;
import com.platform.video.dto.TrendingVideoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling trending/velocity calculations (Feature 27: Trending/Velocity System)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingService {

    private final VideoMetricsRepository videoMetricsRepository;
    private final VideoRepository videoRepository;

    /**
     * Record current metrics for a video (to be called periodically)
     */
    @Transactional
    public void recordVideoMetrics(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Video not found"));

        VideoMetrics metrics = new VideoMetrics();
        metrics.setVideoId(videoId);
        metrics.setRecordedAt(Instant.now());
        metrics.setViewCount(video.getViewCount());
        metrics.setLikeCount(video.getLikeCount());
        metrics.setCommentCount(video.getCommentCount());
        metrics.setShareCount(video.getShareCount());
        // Note: watchTimeSeconds and subscriberGain would need to be tracked separately
        // For now, we'll use placeholder values or zero
        metrics.setWatchTimeSeconds(0L);
        metrics.setSubscriberGain(0L);

        videoMetricsRepository.save(metrics);
        log.debug("Recorded metrics for video {}", videoId);
    }

    /**
     * Calculate trending score for a video based on velocity
     * Trending score = weighted sum of recent growth rates
     */

    @Transactional
    public Double calculateTrendingScore(UUID videoId, int hoursLookback) {
        Instant now = Instant.now();
        Instant lookbackTime = now.minusSeconds(hoursLookback * 3600L);

        List<VideoMetrics> metrics =
                videoMetricsRepository.findByVideoIdAndTimeRange(
                        videoId, lookbackTime, now);

        if (metrics.size() < 2) {
            // Not enough data points to calculate velocity
            return 0.0;
        }

        // Sort by recorded time (oldest first)
        metrics.sort((m1, m2) ->
                m1.getRecordedAt().compareTo(m2.getRecordedAt()));

        // Convert timestamps to a correctly typed Instant array once
        Instant[] timestamps = metrics.stream()
                .map(VideoMetrics::getRecordedAt)
                .toArray(Instant[]::new);

        // Calculate velocity for each metric
        double viewVelocity = calculateVelocity(
                metrics.stream()
                        .mapToLong(VideoMetrics::getViewCount)
                        .toArray(),
                timestamps);

        double likeVelocity = calculateVelocity(
                metrics.stream()
                        .mapToLong(VideoMetrics::getLikeCount)
                        .toArray(),
                timestamps);

        double commentVelocity = calculateVelocity(
                metrics.stream()
                        .mapToLong(VideoMetrics::getCommentCount)
                        .toArray(),
                timestamps);

        double shareVelocity = calculateVelocity(
                metrics.stream()
                        .mapToLong(VideoMetrics::getShareCount)
                        .toArray(),
                timestamps);

        // Weighted combination of velocities
        double trendingScore =
                0.5 * viewVelocity +
                        0.3 * likeVelocity +
                        0.1 * commentVelocity +
                        0.1 * shareVelocity;

        // Ensure non-negative score
        trendingScore = Math.max(0.0, trendingScore);

        // Update the latest metrics record
        VideoMetrics latestMetrics = metrics.get(metrics.size() - 1);
        latestMetrics.setTrendingScore(trendingScore);
        videoMetricsRepository.save(latestMetrics);

        log.debug(
                "Calculated trending score for video {}: {}",
                videoId,
                trendingScore);

        return trendingScore;
    }


    /**
     * Calculate velocity (rate of change) from a series of data points
     */
    private double calculateVelocity(long[] values, Instant[] timestamps) {
        if (values.length < 2) {
            return 0.0;
        }

        // Calculate changes between consecutive points
        long totalChange = 0;
        double totalTimeSeconds = 0.0;

        for (int i = 1; i < values.length; i++) {
            long valueChange = values[i] - values[i-1];
            double timeChangeSeconds = timestamps[i].getEpochSecond() - timestamps[i-1].getEpochSecond();

            if (timeChangeSeconds > 0) {
                totalChange += valueChange;
                totalTimeSeconds += timeChangeSeconds;
            }
        }

        if (totalTimeSeconds == 0) {
            return 0.0;
        }

        // Return average change per second
        return (double) totalChange / totalTimeSeconds;
    }

    /**
     * Get trending videos based on velocity
     */
    @Transactional(readOnly = true)
    public List<TrendingVideoResponse> getTrendingVideos(int limit, int hoursLookback) {
        // Get latest metrics for all videos
        List<VideoMetrics> latestMetrics = videoMetricsRepository.findLatestMetricsForAllVideos();

        // Calculate trending score for each video
        latestMetrics.forEach(metrics -> {
            calculateTrendingScore(metrics.getVideoId(), hoursLookback);
        });

        // Refresh the latest metrics after calculating scores
        latestMetrics = videoMetricsRepository.findLatestMetricsForAllVideos();

        // Sort by trending score (descending) and take top videos
        List<VideoMetrics> sortedMetrics = latestMetrics.stream()
                .sorted((m1, m2) -> Double.compare(m2.getTrendingScore(), m1.getTrendingScore()))
                .limit(limit)
                .collect(Collectors.toList());

        // Convert to response DTOs
        return sortedMetrics.stream()
                .map(this::mapToTrendingVideoResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert VideoMetrics and Video entities to TrendingVideoResponse DTO
     */
    private TrendingVideoResponse mapToTrendingVideoResponse(VideoMetrics metrics) {
        Video video = videoRepository.findById(metrics.getVideoId())
                .orElseThrow(() -> new RuntimeException("Video not found for metrics: " + metrics.getVideoId()));

        return TrendingVideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .thumbnailUrl(video.getThumbnailUrl())
                .creatorId(video.getCreatorId())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .shareCount(video.getShareCount())
                .trendingScore(metrics.getTrendingScore())
                .publishedAt(video.getPublishedAt())
                .lastCalculatedAt(metrics.getRecordedAt())
                .build();
    }
}
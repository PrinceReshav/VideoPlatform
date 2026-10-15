package com.platform.video.health;

import com.platform.video.repository.VideoRepository;
import com.platform.video.repository.VideoMetricsRepository;
import com.platform.video.repository.LiveStreamRepository;
import com.platform.video.repository.OfflineDownloadRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Custom health indicator for Video Service (Feature 39: Service Health/Operations Dashboard)
 * Checks service-specific components beyond basic database connectivity
 */
@Component
public class VideoServiceHealthIndicator implements HealthIndicator {

    private final VideoRepository videoRepository;
    private final VideoMetricsRepository videoMetricsRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final OfflineDownloadRepository offlineDownloadRepository;

    public VideoServiceHealthIndicator(VideoRepository videoRepository,
                                       VideoMetricsRepository videoMetricsRepository,
                                       LiveStreamRepository liveStreamRepository,
                                       OfflineDownloadRepository offlineDownloadRepository) {
        this.videoRepository = videoRepository;
        this.videoMetricsRepository = videoMetricsRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.offlineDownloadRepository = offlineDownloadRepository;
    }

    @Override
    public Health health() {
        try {
            // Check if we can access the video repository
            long videoCount = videoRepository.count();

            // Check if we can access the video metrics repository
            long metricsCount = videoMetricsRepository.count();

            // Check if we can access the live stream repository
            long liveStreamCount = liveStreamRepository.count();

            // Check if we can access the offline download repository
            long offlineDownloadCount = offlineDownloadRepository.count();

            // Check for expired offline downloads that need processing
            long expiredDownloads = offlineDownloadRepository.countByStatus(
                    com.platform.video.entity.OfflineDownload.OfflineDownloadStatus.EXPIRED);

            // Build health status
            return Health.up()
                    .withDetail("videos", videoCount)
                    .withDetail("metrics_records", metricsCount)
                    .withDetail("live_streams", liveStreamCount)
                    .withDetail("offline_downloads", offlineDownloadCount)
                    .withDetail("expired_downloads_pending_cleanup", expiredDownloads)
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
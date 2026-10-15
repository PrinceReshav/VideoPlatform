package com.platform.video.controller;

import com.platform.common.entity.Video;
import com.platform.video.entity.LiveStream;
import com.platform.video.entity.OfflineDownload;
import com.platform.video.repository.VideoRepository;
import com.platform.video.repository.VideoMetricsRepository;
import com.platform.video.repository.LiveStreamRepository;
import com.platform.video.repository.OfflineDownloadRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class OperationsController {

    private final VideoRepository videoRepository;
    private final VideoMetricsRepository videoMetricsRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final OfflineDownloadRepository offlineDownloadRepository;

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getOperationsMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("total_videos", videoRepository.count());
        metrics.put(
                "public_videos",
                videoRepository.countByVisibility(Video.Visibility.PUBLIC)
        );
        metrics.put(
                "private_videos",
                videoRepository.countByVisibility(Video.Visibility.PRIVATE)
        );
        metrics.put(
                "unlisted_videos",
                videoRepository.countByVisibility(Video.Visibility.UNLISTED)
        );

        metrics.put("total_live_streams", liveStreamRepository.count());
        metrics.put(
                "online_live_streams",
                liveStreamRepository.countByStatus(
                        LiveStream.LiveStreamStatus.ONLINE
                )
        );
        metrics.put(
                "offline_live_streams",
                liveStreamRepository.countByStatus(
                        LiveStream.LiveStreamStatus.OFFLINE
                )
        );

        metrics.put("total_offline_downloads", offlineDownloadRepository.count());
        metrics.put(
                "active_offline_downloads",
                offlineDownloadRepository.countByStatus(
                        OfflineDownload.OfflineDownloadStatus.ACTIVE
                )
        );
        metrics.put(
                "expired_offline_downloads",
                offlineDownloadRepository.countByStatus(
                        OfflineDownload.OfflineDownloadStatus.EXPIRED
                )
        );

        metrics.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<Map<String, Object>> getRecentActivity() {
        Map<String, Object> activity = new HashMap<>();
        Instant twentyFourHoursAgo = Instant.now().minusSeconds(24 * 3600);

        activity.put(
                "videos_published_24h",
                videoRepository.countByCreatedAtAfter(twentyFourHoursAgo)
        );
        activity.put(
                "live_streams_started_24h",
                liveStreamRepository.countByStartedAtAfter(twentyFourHoursAgo)
        );
        activity.put(
                "offline_downloads_started_24h",
                offlineDownloadRepository.countByCreatedAtAfter(twentyFourHoursAgo)
        );

        return ResponseEntity.ok(activity);
    }
}
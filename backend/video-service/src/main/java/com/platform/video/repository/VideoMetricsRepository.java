package com.platform.video.repository;

import com.platform.video.entity.VideoMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for VideoMetrics entities (Feature 27: Trending/Velocity System)
 */
@Repository
public interface VideoMetricsRepository extends JpaRepository<VideoMetrics, UUID> {

    /** Find metrics by video ID */
    List<VideoMetrics> findByVideoId(UUID videoId);

    /** Find metrics by video ID ordered by recorded time (newest first) */
    List<VideoMetrics> findByVideoIdOrderByRecordedAtDesc(UUID videoId);

    /** Find metrics recorded after a given time */
    List<VideoMetrics> findByRecordedAtAfter(Instant recordedAt);

    /** Find latest metrics for each video */
    @Query("SELECT vm FROM VideoMetrics vm WHERE vm.recordedAt = (" +
           "SELECT MAX(vm2.recordedAt) FROM VideoMetrics vm2 WHERE vm2.videoId = vm.videoId)")
    List<VideoMetrics> findLatestMetricsForAllVideos();

    /** Find metrics for a video within a time range */
    @Query("SELECT vm FROM VideoMetrics vm WHERE vm.videoId = :videoId " +
           "AND vm.recordedAt >= :startTime AND vm.recordedAt <= :endTime " +
           "ORDER BY vm.recordedAt ASC")
    List<VideoMetrics> findByVideoIdAndTimeRange(
            @Param("videoId") UUID videoId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
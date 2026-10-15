package com.platform.analytics.repository;

import com.platform.analytics.dto.ChannelAnalyticsSummary;
import com.platform.analytics.dto.VideoAnalyticsSummary;
import com.platform.analytics.entity.VideoAnalyticsDaily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoAnalyticsDailyRepository extends JpaRepository<VideoAnalyticsDaily, UUID> {

    Optional<VideoAnalyticsDaily> findByVideoIdAndDate(UUID videoId, LocalDate date);

    List<VideoAnalyticsDaily> findByVideoIdAndDateBetween(UUID videoId, LocalDate startDate, LocalDate endDate);

    List<VideoAnalyticsDaily> findByChannelIdAndDateBetween(UUID channelId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT new com.platform.analytics.dto.VideoAnalyticsSummary(" +
            "SUM(v.views), SUM(v.watchTimeSeconds), SUM(v.likes), SUM(v.comments), " +
            "SUM(v.shares), SUM(v.subscribersGained), SUM(v.subscribersLost), " +
            "AVG(v.avgViewDurationSeconds), AVG(v.avgViewPercentage), " +
            "SUM(v.uniqueViewers), MAX(v.viewersAtPeakConcurrent), AVG(v.averageCompletionRatePercentage) " +
            ") FROM VideoAnalyticsDaily v WHERE v.videoId = :videoId AND v.date BETWEEN :startDate AND :endDate")
    VideoAnalyticsSummary getSummaryByVideoIdAndDateRange(@Param("videoId") UUID videoId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT new com.platform.analytics.dto.ChannelAnalyticsSummary(" +
            "SUM(c.views), SUM(c.watchTimeSeconds), SUM(c.likes), SUM(c.comments), " +
            "SUM(c.shares), SUM(c.subscribersGained), SUM(c.subscribersLost), " +
            "SUM(c.videosPublished), SUM(c.shortsPublished), SUM(c.liveStreamMinutes), " +
            "MAX(c.peakConcurrentLiveViewers) " +
            ") FROM ChannelAnalyticsDaily c WHERE c.channelId = :channelId AND c.date BETWEEN :startDate AND :endDate")
    ChannelAnalyticsSummary getSummaryByChannelIdAndDateRange(@Param("channelId") UUID channelId,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);

    Page<VideoAnalyticsDaily> findByVideoIdOrderByDateDesc(UUID videoId, Pageable pageable);

    Page<VideoAnalyticsDaily> findByChannelIdOrderByDateDesc(UUID channelId, Pageable pageable);
}
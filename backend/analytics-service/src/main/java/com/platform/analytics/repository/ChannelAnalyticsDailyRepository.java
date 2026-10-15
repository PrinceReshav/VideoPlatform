package com.platform.analytics.repository;

import com.platform.analytics.dto.ChannelAnalyticsSummary;
import com.platform.analytics.entity.ChannelAnalyticsDaily;
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
public interface ChannelAnalyticsDailyRepository extends JpaRepository<ChannelAnalyticsDaily, UUID> {

    Optional<ChannelAnalyticsDaily> findByChannelIdAndDate(UUID channelId, LocalDate date);

    List<ChannelAnalyticsDaily> findByChannelIdAndDateBetween(UUID channelId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT new com.platform.analytics.dto.ChannelAnalyticsSummary(" +
            "SUM(c.views), SUM(c.watchTimeSeconds), SUM(c.likes), SUM(c.comments), " +
            "SUM(c.shares), SUM(c.subscribersGained), SUM(c.subscribersLost), " +
            "SUM(c.videosPublished), " +
            "SUM(c.uniqueViewers), MAX(c.viewersAtPeakConcurrent), AVG(c.averageCompletionRatePercentage), " +
            "SUM(c.shortsPublished), SUM(c.liveStreamMinutes), MAX(c.peakConcurrentLiveViewers) " +
            ") FROM ChannelAnalyticsDaily c WHERE c.channelId = :channelId AND c.date BETWEEN :startDate AND :endDate")
    ChannelAnalyticsSummary getSummaryByChannelIdAndDateRange(@Param("channelId") UUID channelId,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);

    Page<ChannelAnalyticsDaily> findByChannelIdOrderByDateDesc(UUID channelId, Pageable pageable);
}
package com.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Enhanced Daily Channel Analytics DTO for Feature 6: Creator Analytics
 * Contains comprehensive daily metrics for channel performance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChannelAnalytics {
    private LocalDate date;
    private long views;
    private long watchTimeSeconds;
    private long likes;
    private long comments;
    private long shares;
    private long subscribersGained;
    private long subscribersLost;
    private int videosPublished;
    private long totalVideos;
    private Double estimatedRevenue;

    // Enhanced metrics for Feature 6
    private long uniqueViewers;
    private long viewersAtPeakConcurrent;
    private Integer averageCompletionRatePercentage;
    private int shortsPublished;
    private long liveStreamMinutes;
    private long peakConcurrentLiveViewers;

    // Traffic Sources
    private Map<String, Long> trafficSources;

    // Geography
    private Map<String, Long> viewsByCountry;
    private Map<String, Long> viewsByCity;

    // Devices & Operating Systems
    private Map<String, Long> viewsByDeviceType;
    private Map<String, Long> viewsByOperatingSystem;
    private Map<String, Long> viewsByBrowser;

    // Playback Quality
    private Map<String, Long> viewsByQuality;
    private Long averageBitrateKbps;
    private Long rebufferCount;
    private Double rebufferRatio;

    // Revenue metrics
    private Double adRevenue;
    private Double premiumRevenue;
    private Double membershipRevenue;
    private Long adImpressions;
    private Long adClicks;
    private Long membershipsGained;
    private Long membershipsLost;
    private Double cpm;

    // Content Performance
    private Long averageViewsPerVideo;
    private Long averageWatchTimePerVideo;
    private Long engagementRatePercentage;
    private Long subscriberGrowthRatePercentage;

    // Audience Demographics
    private Map<String, Long> audienceByAgeGroup;
    private Map<String, Long> audienceByGender;

    // Engagement Quality
    private Long likesPerView;
    private Long commentsPerView;
    private Long sharesPerView;
    private Long subscribersGainedPerView;

    // Retention
    private Map<Integer, Double> retentionAtIntervals;
}
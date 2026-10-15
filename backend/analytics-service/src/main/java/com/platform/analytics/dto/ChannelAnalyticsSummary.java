package com.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Channel Analytics Summary with comprehensive metrics for Feature 6: Creator Analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelAnalyticsSummary {
    // Basic metrics (existing)
    private Long totalViews;
    private Long totalWatchTimeSeconds;
    private Long totalLikes;
    private Long totalComments;
    private Long totalShares;
    private Long totalSubscribersGained;
    private Long totalSubscribersLost;
    private Long totalVideosPublished;

    // Enhanced metrics for Feature 6
    private Long uniqueViewers;
    private Long viewersAtPeakConcurrent;
    private Integer averageCompletionRatePercentage;
    private Long totalShortsPublished;
    private Long totalLiveStreamMinutes;
    private Long peakConcurrentLiveViewers;

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

    // Revenue (for monetization - Feature 32)
    private Double estimatedRevenue;
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
    private Long engagementRatePercentage; // (likes+comments+shares)/views * 100
    private Long subscriberGrowthRatePercentage;

    // Audience Demographics
    private Map<String, Long> audienceByAgeGroup; // e.g., "13-17": 1000, "18-24": 5000
    private Map<String, Long> audienceByGender; // e.g., "M": 3000, "F": 2500

    // Engagement Quality
    private Long likesPerView;
    private Long commentsPerView;
    private Long sharesPerView;
    private Long subscribersGainedPerView;

    // Retention
    private Map<Integer, Double> retentionAtIntervals;
}
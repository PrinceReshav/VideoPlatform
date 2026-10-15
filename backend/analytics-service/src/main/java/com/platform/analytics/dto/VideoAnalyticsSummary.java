package com.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Video Analytics Summary with comprehensive metrics for Feature 6: Creator Analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalyticsSummary {
    // Basic metrics (existing)
    private Long totalViews;
    private Long totalWatchTimeSeconds;
    private Long totalLikes;
    private Long totalComments;
    private Long totalShares;
    private Long totalSubscribersGained;
    private Long totalSubscribersLost;
    private Double avgViewDurationSeconds;
    private Double avgViewPercentage;

    // Enhanced metrics for Feature 6
    private Long uniqueViewers;
    private Long viewersAtPeakConcurrent;
    private Integer averageCompletionRatePercentage;

    // Traffic Sources
    private Map<String, Long> trafficSources; // e.g., "youtube_search": 1000, "external": 500

    // Geography
    private Map<String, Long> viewsByCountry; // e.g., "US": 5000, "IN": 3000
    private Map<String, Long> viewsByCity; // Top cities

    // Devices & Operating Systems
    private Map<String, Long> viewsByDeviceType; // mobile, desktop, tablet, tv
    private Map<String, Long> viewsByOperatingSystem; // iOS, Android, Windows, macOS
    private Map<String, Long> viewsByBrowser; // Chrome, Safari, Firefox, etc.

    // Playback Quality
    private Map<String, Long> viewsByQuality; // 144p, 240p, 360p, 480p, 720p, 1080p, 4K
    private Long averageBitrateKbps;
    private Long rebufferCount;
    private Double rebufferRatio; // % of time spent rebuffering

    // Revenue (for monetization - Feature 32)
    private Double estimatedRevenue;
    private Double adRevenue;
    private Double premiumRevenue;
    private Long adImpressions;
    private Long adClicks;
    private Double cpm; // Cost per mille

    // Engagement Quality
    private Long likesPerView;
    private Long commentsPerView;
    private Long sharesPerView;
    private Long subscribersGainedPerView;

    // Retention Curve (simplified - key points)
    private Map<Integer, Double> retentionAtIntervals; // e.g., {15: 0.8, 30: 0.6, 60: 0.4} - percentage remaining
}
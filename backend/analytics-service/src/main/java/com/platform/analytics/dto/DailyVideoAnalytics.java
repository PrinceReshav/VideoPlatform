package com.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Enhanced Daily Video Analytics DTO for Feature 6: Creator Analytics
 * Contains comprehensive daily metrics for video performance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyVideoAnalytics {
    private LocalDate date;
    private long views;
    private long watchTimeSeconds;
    private long likes;
    private long comments;
    private long shares;
    private long subscribersGained;
    private long subscribersLost;
    private Double avgViewDurationSeconds;
    private Double avgViewPercentage;

    // Enhanced metrics for Feature 6
    private long uniqueViewers;
    private long viewersAtPeakConcurrent;
    private Integer averageCompletionRatePercentage;

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
    private Double estimatedRevenue;
    private Double adRevenue;
    private Double premiumRevenue;
    private Long adImpressions;
    private Long adClicks;
    private Double cpm;

    // Engagement Quality
    private Long likesPerView;
    private Long commentsPerView;
    private Long sharesPerView;
    private Long subscribersGainedPerView;

    // Retention
    private Map<Integer, Double> retentionAtIntervals;

    // Audience Demographics
    private Map<String, Long> audienceByAgeGroup;
    private Map<String, Long> audienceByGender;
}
package com.platform.analytics.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Enhanced Video Analytics Daily entity for Feature 6: Creator Analytics
 * Stores comprehensive metrics for video performance analysis
 */
@Entity
@Table(name = "video_analytics_daily", schema = "analytics_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_video_analytics_daily_video_date", columnNames = {"video_id", "date"})
        },
        indexes = {
                @Index(name = "idx_video_analytics_daily_video", columnList = "video_id"),
                @Index(name = "idx_video_analytics_daily_date", columnList = "date"),
                @Index(name = "idx_video_analytics_daily_channel", columnList = "channel_id")
        })
@Getter
@Setter
public class VideoAnalyticsDaily extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // Basic metrics (existing)
    @Column(name = "views", nullable = false)
    private long views = 0;

    @Column(name = "watch_time_seconds", nullable = false)
    private long watchTimeSeconds = 0;

    @Column(name = "likes", nullable = false)
    private long likes = 0;

    @Column(name = "comments", nullable = false)
    private long comments = 0;

    @Column(name = "shares", nullable = false)
    private long shares = 0;

    @Column(name = "subscribers_gained", nullable = false)
    private long subscribersGained = 0;

    @Column(name = "subscribers_lost", nullable = false)
    private long subscribersLost = 0;

    @Column(name = "avg_view_duration_seconds")
    private Double avgViewDurationSeconds;

    @Column(name = "avg_view_percentage")
    private Double avgViewPercentage;

    @Column(name = "impressions")
    private long impressions = 0;

    @Column(name = "click_through_rate")
    private Double clickThroughRate;

    // Enhanced metrics for Feature 6

    // Unique viewers and peak metrics
    @Column(name = "unique_viewers")
    private long uniqueViewers = 0;

    @Column(name = "viewers_at_peak_concurrent")
    private long viewersAtPeakConcurrent = 0;

    @Column(name = "average_completion_rate_percentage")
    private Integer averageCompletionRatePercentage;

    // Traffic Sources (JSON)
    @Column(name = "traffic_sources", columnDefinition = "TEXT")
    private String trafficSources; // JSON map: {"youtube_search": 1000, "external": 500}

    // Geography (JSON)
    @Column(name = "views_by_country", columnDefinition = "TEXT")
    private String viewsByCountry; // JSON map: {"US": 5000, "IN": 3000}

    @Column(name = "views_by_city", columnDefinition = "TEXT")
    private String viewsByCity; // JSON map of top cities

    // Devices & Operating Systems (JSON)
    @Column(name = "views_by_device_type", columnDefinition = "TEXT")
    private String viewsByDeviceType; // mobile, desktop, tablet, tv

    @Column(name = "views_by_operating_system", columnDefinition = "TEXT")
    private String viewsByOperatingSystem; // iOS, Android, Windows, macOS

    @Column(name = "views_by_browser", columnDefinition = "TEXT")
    private String viewsByBrowser; // Chrome, Safari, Firefox, etc.

    // Playback Quality (JSON)
    @Column(name = "views_by_quality", columnDefinition = "TEXT")
    private String viewsByQuality; // 144p, 240p, 360p, 480p, 720p, 1080p, 4K

    @Column(name = "average_bitrate_kbps")
    private Long averageBitrateKbps;

    @Column(name = "rebuffer_count")
    private Long rebufferCount;

    @Column(name = "rebuffer_ratio")
    private Double rebufferRatio; // % of time spent rebuffering

    // Revenue metrics (for monetization - Feature 32)
    @Column(name = "estimated_revenue")
    private Double estimatedRevenue;

    @Column(name = "ad_revenue")
    private Double adRevenue;

    @Column(name = "premium_revenue")
    private Double premiumRevenue;

    @Column(name = "ad_impressions")
    private Long adImpressions;

    @Column(name = "ad_clicks")
    private Long adClicks;

    @Column(name = "cpm")
    private Double cpm; // Cost per mille

    // Engagement Quality metrics
    @Column(name = "likes_per_view")
    private Long likesPerView;

    @Column(name = "comments_per_view")
    private Long commentsPerView;

    @Column(name = "shares_per_view")
    private Long sharesPerView;

    @Column(name = "subscribers_gained_per_view")
    private Long subscribersGainedPerView;

    // Retention Curve (JSON - key points)
    @Column(name = "retention_at_intervals", columnDefinition = "TEXT")
    private String retentionAtIntervals; // JSON map: {15: 0.8, 30: 0.6, 60: 0.4}

    // Audience Demographics (JSON)
    @Column(name = "audience_by_age_group", columnDefinition = "TEXT")
    private String audienceByAgeGroup; // e.g., {"13-17": 1000, "18-24": 5000}

    @Column(name = "audience_by_gender", columnDefinition = "TEXT")
    private String audienceByGender; // e.g., {"M": 3000, "F": 2500}
}
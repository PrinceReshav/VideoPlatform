package com.platform.analytics.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Enhanced Channel Analytics Daily entity for Feature 6: Creator Analytics
 * Stores comprehensive metrics for channel performance analysis
 */
@Entity
@Table(name = "channel_analytics_daily", schema = "analytics_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_channel_analytics_daily_channel_date", columnNames = {"channel_id", "date"})
        },
        indexes = {
                @Index(name = "idx_channel_analytics_daily_channel", columnList = "channel_id"),
                @Index(name = "idx_channel_analytics_daily_date", columnList = "date")
        })
@Getter
@Setter
public class ChannelAnalyticsDaily extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

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

    @Column(name = "videos_published", nullable = false)
    private int videosPublished = 0;

    @Column(name = "total_videos", nullable = false)
    private int totalVideos = 0;

    @Column(name = "estimated_revenue")
    private Double estimatedRevenue;

    // Enhanced metrics for Feature 6

    // Unique viewers and peak metrics
    @Column(name = "unique_viewers")
    private long uniqueViewers = 0;

    @Column(name = "viewers_at_peak_concurrent")
    private long viewersAtPeakConcurrent = 0;

    @Column(name = "average_completion_rate_percentage")
    private Integer averageCompletionRatePercentage;

    @Column(name = "shorts_published")
    private int shortsPublished = 0;

    @Column(name = "live_stream_minutes")
    private long liveStreamMinutes = 0;

    @Column(name = "peak_concurrent_live_viewers")
    private long peakConcurrentLiveViewers = 0;

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
    @Column(name = "ad_revenue")
    private Double adRevenue;

    @Column(name = "premium_revenue")
    private Double premiumRevenue;

    @Column(name = "membership_revenue")
    private Double membershipRevenue;

    @Column(name = "ad_impressions")
    private Long adImpressions;

    @Column(name = "ad_clicks")
    private Long adClicks;

    @Column(name = "memberships_gained")
    private Long membershipsGained;

    @Column(name = "memberships_lost")
    private Long membershipsLost;

    @Column(name = "cpm")
    private Double cpm; // Cost per mille

    // Content Performance
    @Column(name = "average_views_per_video")
    private Long averageViewsPerVideo;

    @Column(name = "average_watch_time_per_video")
    private Long averageWatchTimePerVideo;

    @Column(name = "engagement_rate_percentage")
    private Long engagementRatePercentage; // (likes+comments+shares)/views * 100

    @Column(name = "subscriber_growth_rate_percentage")
    private Long subscriberGrowthRatePercentage;

    // Audience Demographics (JSON)
    @Column(name = "audience_by_age_group", columnDefinition = "TEXT")
    private String audienceByAgeGroup; // e.g., {"13-17": 1000, "18-24": 5000}

    @Column(name = "audience_by_gender", columnDefinition = "TEXT")
    private String audienceByGender; // e.g., {"M": 3000, "F": 2500}

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
}
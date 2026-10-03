package com.platform.analytics.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

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

    @Column(name = "traffic_sources", columnDefinition = "TEXT")
    private String trafficSources; // JSON map of source -> count

    @Column(name = "geography", columnDefinition = "TEXT")
    private String geography; // JSON map of country -> count

    @Column(name = "devices", columnDefinition = "TEXT")
    private String devices; // JSON map of device -> count
}
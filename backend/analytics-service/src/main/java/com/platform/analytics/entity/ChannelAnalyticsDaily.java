package com.platform.analytics.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

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
}
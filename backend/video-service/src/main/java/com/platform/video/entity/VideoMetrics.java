package com.platform.video.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking video metrics over time (Feature 27: Trending/Velocity System).
 * Stores periodic snapshots of video engagement metrics for velocity calculations.
 */
@Entity
@Table(
        name = "video_metrics",
        schema = "video_schema",
        indexes = {
                @Index(name = "idx_video_metrics_video", columnList = "video_id"),
                @Index(name = "idx_video_metrics_recorded_at", columnList = "recorded_at"),
                @Index(
                        name = "idx_video_metrics_video_recorded",
                        columnList = "video_id, recorded_at"
                )
        }
)
@Getter
@Setter
public class VideoMetrics extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The video these metrics belong to. */
    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    /** Timestamp when these metrics were recorded. */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /** View count at the time of recording. */
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    /** Like count at the time of recording. */
    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    /** Comment count at the time of recording. */
    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;

    /** Share count at the time of recording. */
    @Column(name = "share_count", nullable = false)
    private Long shareCount = 0L;

    /** Watch time in seconds at the time of recording. */
    @Column(name = "watch_time_seconds", nullable = false)
    private Long watchTimeSeconds = 0L;

    /** Subscriber gain attributed to this video at the time of recording. */
    @Column(name = "subscriber_gain", nullable = false)
    private Long subscriberGain = 0L;

    /** Trending score calculated from velocity. */
    @Column(name = "trending_score")
    private Double trendingScore = 0.0;
}
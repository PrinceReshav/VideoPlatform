package com.platform.engagement.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "watch_history", schema = "engagement_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_watch_history_user_video", columnNames = {"user_id", "video_id"})
        },
        indexes = {
                @Index(name = "idx_watch_history_user", columnList = "user_id"),
                @Index(name = "idx_watch_history_video", columnList = "video_id"),
                @Index(name = "idx_watch_history_updated_at", columnList = "updated_at"),
                @Index(name = "idx_watch_history_user_updated", columnList = "user_id, updated_at")
        })
@Getter
@Setter
public class WatchHistory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "position_seconds", nullable = false)
    private int positionSeconds = 0;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "progress_percent")
    private int progressPercent = 0;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "watch_count", nullable = false)
    private int watchCount = 1;

    @Column(name = "last_watched_at", nullable = false)
    private Instant lastWatchedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
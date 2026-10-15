package com.platform.engagement.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for saved videos (Feature 24: Watch Later)
 * Tracks videos that users have saved to watch later
 */
@Entity
@Table(name = "saved_videos", schema = "engagement_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_saved_videos_user_video", columnNames = {"user_id", "video_id"})
        },
        indexes = {
                @Index(name = "idx_saved_videos_user", columnList = "user_id"),
                @Index(name = "idx_saved_videos_video", columnList = "video_id"),
                @Index(name = "idx_saved_videos_saved_at", columnList = "saved_at")
        })
@Getter
@Setter
public class SavedVideo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    /** When the video was saved for later */
    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt = Instant.now();
}
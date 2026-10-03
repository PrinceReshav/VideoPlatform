package com.platform.engagement.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "views", schema = "engagement_schema",
        indexes = {
                @Index(name = "idx_views_video", columnList = "video_id"),
                @Index(name = "idx_views_user", columnList = "user_id"),
                @Index(name = "idx_views_created_at", columnList = "created_at"),
                @Index(name = "idx_views_video_user", columnList = "video_id, user_id")
        })
@Getter
@Setter
public class View extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "position_seconds")
    private Integer positionSeconds;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent_hash", length = 64)
    private String userAgentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
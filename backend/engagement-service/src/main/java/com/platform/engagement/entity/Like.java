package com.platform.engagement.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "likes", schema = "engagement_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_likes_user_video", columnNames = {"user_id", "video_id"})
        },
        indexes = {
                @Index(name = "idx_likes_video", columnList = "video_id"),
                @Index(name = "idx_likes_user", columnList = "user_id"),
                @Index(name = "idx_likes_created_at", columnList = "created_at")
        })
@Getter
@Setter
public class Like extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false, updatable = false)
    private UUID videoId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
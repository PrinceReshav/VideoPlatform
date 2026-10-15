package com.platform.engagement.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments", schema = "engagement_schema",
        indexes = {
                @Index(name = "idx_comments_video", columnList = "video_id"),
                @Index(name = "idx_comments_user", columnList = "user_id"),
                @Index(name = "idx_comments_parent", columnList = "parent_id"),
                @Index(name = "idx_comments_created_at", columnList = "created_at"),
                @Index(name = "idx_comments_hidden", columnList = "hidden"),
                @Index(name = "idx_comments_timestamp", columnList = "timestamp_seconds")
        })
@Getter
@Setter
public class Comment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "content", nullable = false, length = 5000)
    private String content;

    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;

    @Column(name = "hidden_by")
    private UUID hiddenBy;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "reply_count", nullable = false)
    private int replyCount = 0;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    /** Timestamp in video (in seconds) when this comment was made */
    @Column(name = "timestamp_seconds")
    private Integer timestampSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
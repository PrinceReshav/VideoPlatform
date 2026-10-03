package com.platform.notification.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "notification_schema",
        indexes = {
                @Index(name = "idx_notifications_user", columnList = "user_id"),
                @Index(name = "idx_notifications_type", columnList = "type"),
                @Index(name = "idx_notifications_read", columnList = "read"),
                @Index(name = "idx_notifications_created_at", columnList = "created_at"),
                @Index(name = "idx_notifications_user_read", columnList = "user_id, read")
        })
@Getter
@Setter
public class Notification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private Type type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "priority", nullable = false)
    private int priority = 0; // 0=normal, 1=high, 2=urgent

    public enum Type {
        NEW_VIDEO,
        NEW_COMMENT,
        COMMENT_REPLY,
        COMMENT_LIKE,
        NEW_SUBSCRIBER,
        VIDEO_LIKE,
        VIDEO_MILESTONE,
        CHANNEL_MILESTONE,
        SYSTEM_ANNOUNCEMENT,
        MODERATION_ACTION,
        UPLOAD_COMPLETE,
        PROCESSING_COMPLETE,
        PROCESSING_FAILED,
        LIVE_STARTING,
        PAYMENT_RECEIVED
    }
}
package com.platform.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification entity for Feature 17: Notification Center enhancement
 * Represents a notification for a user
 */
@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read"),
                @Index(name = "idx_notifications_type", columnList = "notification_type")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE notifications SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    /** The user who receives the notification */
    @Column(nullable = false)
    private UUID userId;

    /** The type of notification */
    @Column(length = 50, nullable = false)
    private String notificationType; // VIDEO_UPLOADED, LIVE_STARTED, COMMENT, LIKE, SUBSCRIPTION, etc.

    /** The title of the notification */
    @Column(length = 200, nullable = false)
    private String title;

    /** The message/content of the notification */
    @Column(length = 1000)
    private String message;

    /** Whether the notification has been read */
    @Column(nullable = false)
    private boolean isRead = false;

    /** Whether the notification has been archived */
    @Column(nullable = false)
    private boolean isArchived = false;

    /** The entity that triggered this notification (optional) */
    private UUID entityId;

    /** The type of entity that triggered this notification (optional) */
    @Column(length = 50)
    private String entityType; // VIDEO, CHANNEL, COMMENT, etc.

    /** Additional data for the notification (JSON format) */
    @Column(columnDefinition = "jsonb")
    private String data;

    /** Timestamp when the notification was created */
    private Instant createdAt;

    /** Timestamp when the notification was read */
    private Instant readAt;

    /** Soft delete flag */
    private boolean deleted = false;
}
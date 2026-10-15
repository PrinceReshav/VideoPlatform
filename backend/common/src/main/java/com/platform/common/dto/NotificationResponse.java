package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for notification responses (Feature 17: Notification Center enhancement)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    /** The unique identifier of the notification */
    private UUID id;

    /** The user who receives the notification */
    private UUID userId;

    /** The type of notification */
    private String notificationType; // VIDEO_UPLOADED, LIVE_STARTED, COMMENT, LIKE, SUBSCRIPTION, etc.

    /** The title of the notification */
    private String title;

    /** The message/content of the notification */
    private String message;

    /** Whether the notification has been read */
    private boolean isRead;

    /** Whether the notification has been archived */
    private boolean isArchived;

    /** The entity that triggered this notification (optional) */
    private UUID entityId;

    /** The type of entity that triggered this notification (optional) */
    private String entityType; // VIDEO, CHANNEL, COMMENT, etc.

    /** Additional data for the notification (JSON format) */
    private String data;

    /** Timestamp when the notification was created */
    private Instant createdAt;

    /** Timestamp when the notification was read */
    private Instant readAt;
}
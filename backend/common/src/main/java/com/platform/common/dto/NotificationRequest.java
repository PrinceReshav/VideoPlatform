package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for notification requests (Feature 17: Notification Center enhancement)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    /** The user who receives the notification */
    @NotNull
    private UUID userId;

    /** The type of notification */
    @NotBlank
    @Size(max = 50)
    private String notificationType; // VIDEO_UPLOADED, LIVE_STARTED, COMMENT, LIKE, SUBSCRIPTION, etc.

    /** The title of the notification */
    @NotBlank
    @Size(max = 200)
    private String title;

    /** The message/content of the notification */
    @Size(max = 1000)
    private String message;

    /** The entity that triggered this notification (optional) */
    private UUID entityId;

    /** The type of entity that triggered this notification (optional) */
    @Size(max = 50)
    private String entityType; // VIDEO, CHANNEL, COMMENT, etc.

    /** Additional data for the notification (JSON format) */
    private String data;
}
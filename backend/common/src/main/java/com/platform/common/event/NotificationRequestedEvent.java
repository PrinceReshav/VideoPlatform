package com.platform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.platform.common.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestedEvent {
    private UUID notificationId;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private UUID referenceId;
    private String referenceType;
    private int priority;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}

package com.platform.notification.dto;

import com.platform.notification.entity.Notification;
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
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private Notification.Type type;
    private String title;
    private String message;
    private String actionUrl;
    private UUID referenceId;
    private String referenceType;
    private boolean read;
    private Instant readAt;
    private int priority;
    private Instant createdAt;
}
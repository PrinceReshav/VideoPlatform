package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.dto.NotificationRequest;
import com.platform.common.dto.NotificationResponse;
import com.platform.common.entity.Notification;
import com.platform.common.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for notification management (Feature 17: Notification Center enhancement)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Create a new notification
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody NotificationRequest request) {

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .notificationType(request.getNotificationType())
                .title(request.getTitle())
                .message(request.getMessage())
                .entityId(request.getEntityId())
                .entityType(request.getEntityType())
                .data(request.getData())
                .build();

        Notification savedNotification = notificationService.createNotification(notification);
        NotificationResponse response = mapToResponse(savedNotification);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get a notification by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(
            @PathVariable UUID id) {

        Notification notification = notificationService.getNotificationById(id);
        NotificationResponse response = mapToResponse(notification);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get notifications by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByUserId(
            @PathVariable UUID userId) {

        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        List<NotificationResponse> responses = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get unread notifications by user ID
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotificationsByUserId(
            @PathVariable UUID userId) {

        List<Notification> notifications = notificationService.getUnreadNotificationsByUserId(userId);
        List<NotificationResponse> responses = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get notifications by user ID and notification type
     */
    @GetMapping("/user/{userId}/type/{notificationType}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByUserIdAndNotificationType(
            @PathVariable UUID userId,
            @PathVariable String notificationType) {

        List<Notification> notifications = notificationService.getNotificationsByUserIdAndNotificationType(userId, notificationType);
        List<NotificationResponse> responses = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get notifications by user ID and timestamp range
     */
    @GetMapping("/user/{userId}/timestamp-range")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByUserIdAndTimestampRange(
            @PathVariable UUID userId,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {

        List<Notification> notifications = notificationService.getNotificationsByUserIdAndTimestampRange(userId, startTime, endTime);
        List<NotificationResponse> responses = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get unread notifications by user ID and timestamp range
     */
    @GetMapping("/user/{userId}/unread/timestamp-range")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotificationsByUserIdAndTimestampRange(
            @PathVariable UUID userId,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {

        List<Notification> notifications = notificationService.getUnreadNotificationsByUserIdAndTimestampRange(userId, startTime, endTime);
        List<NotificationResponse> responses = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get notifications by entity ID and entity type
     */
    @GetMapping("/entity/{entityId}/{entityType}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByEntityIdAndEntityType(
            @PathVariable UUID entityId,
            @PathVariable String entityType) {

        List<Notification> notifications = notificationService.getNotificationsByEntityIdAndEntityType(entityId, entityType);
        List<NotificationResponse> responses = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id) {

        Notification notification = notificationService.markAsRead(id);
        NotificationResponse response = mapToResponse(notification);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @PathVariable UUID userId) {

        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Archive a notification
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<NotificationResponse>> archive(
            @PathVariable UUID id) {

        Notification notification = notificationService.archive(id);
        NotificationResponse response = mapToResponse(notification);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID id) {

        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get notifications with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByCriteria(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isArchived,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // For simplicity, we'll get all matching results without pagination in this example
        // In a real implementation, you'd want to add pagination support to the service method
        List<Notification> notifications = notificationService.getNotificationsByCriteria(
                userId, notificationType, isRead, isArchived, entityId, entityType, startTime, endTime);
        List<NotificationResponse> responses = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get count of unread notifications for a user
     */
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @PathVariable UUID userId) {

        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Map Notification entity to NotificationResponse DTO
     */
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .isArchived(notification.isArchived())
                .entityId(notification.getEntityId())
                .entityType(notification.getEntityType())
                .data(notification.getData())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
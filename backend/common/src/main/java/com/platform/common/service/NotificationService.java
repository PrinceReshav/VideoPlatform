package com.platform.common.service;

import com.platform.common.entity.Notification;
import com.platform.common.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling notifications.
 *
 * Feature 17: Notification Center enhancement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Create a new notification.
     */
    @Transactional
    public Notification createNotification(
            Notification notification) {

        if (notification == null) {
            throw new IllegalArgumentException(
                    "Notification must not be null"
            );
        }

        if (notification.getUserId() == null) {
            throw new IllegalArgumentException(
                    "Notification userId must not be null"
            );
        }

        if (notification.getNotificationType() == null
                || notification.getNotificationType().isBlank()) {

            throw new IllegalArgumentException(
                    "Notification type must not be null or blank"
            );
        }

        if (notification.getTitle() == null
                || notification.getTitle().isBlank()) {

            throw new IllegalArgumentException(
                    "Notification title must not be null or blank"
            );
        }

        /*
         * Only set createdAt when the caller has not already
         * supplied one.
         */
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }

        Notification savedNotification =
                notificationRepository.save(notification);

        log.info(
                "Created notification {} for user {} of type {}",
                savedNotification.getId(),
                savedNotification.getUserId(),
                savedNotification.getNotificationType()
        );

        return savedNotification;
    }

    /**
     * Get a notification by ID.
     */
    @Transactional(readOnly = true)
    public Notification getNotificationById(UUID id) {

        validateId(id, "Notification ID");

        return notificationRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Notification not found with ID: " + id
                        )
                );
    }

    /**
     * Get notifications by user ID.
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByUserId(
            UUID userId) {

        validateId(userId, "User ID");

        return notificationRepository.findByUserId(userId);
    }

    /**
     * Get unread notifications by user ID.
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsByUserId(
            UUID userId) {

        validateId(userId, "User ID");

        return notificationRepository
                .findByUserIdAndIsReadFalse(userId);
    }

    /**
     * Get notifications by user ID and notification type.
     */
    @Transactional(readOnly = true)
    public List<Notification>
    getNotificationsByUserIdAndNotificationType(
            UUID userId,
            String notificationType) {

        validateId(userId, "User ID");

        if (notificationType == null
                || notificationType.isBlank()) {

            throw new IllegalArgumentException(
                    "Notification type must not be null or blank"
            );
        }

        return notificationRepository
                .findByUserIdAndNotificationType(
                        userId,
                        notificationType
                );
    }

    /**
     * Get notifications by user ID and timestamp range.
     */
    @Transactional(readOnly = true)
    public List<Notification>
    getNotificationsByUserIdAndTimestampRange(
            UUID userId,
            Instant startTime,
            Instant endTime) {

        validateId(userId, "User ID");
        validateTimeRange(startTime, endTime);

        return notificationRepository
                .findByUserIdAndCreatedAtBetween(
                        userId,
                        startTime,
                        endTime
                );
    }

    /**
     * Get unread notifications by user ID and timestamp range.
     */
    @Transactional(readOnly = true)
    public List<Notification>
    getUnreadNotificationsByUserIdAndTimestampRange(
            UUID userId,
            Instant startTime,
            Instant endTime) {

        validateId(userId, "User ID");
        validateTimeRange(startTime, endTime);

        return notificationRepository
                .findByUserIdAndIsReadFalseAndCreatedAtBetween(
                        userId,
                        startTime,
                        endTime
                );
    }

    /**
     * Get notifications by entity ID and entity type.
     */
    @Transactional(readOnly = true)
    public List<Notification>
    getNotificationsByEntityIdAndEntityType(
            UUID entityId,
            String entityType) {

        validateId(entityId, "Entity ID");

        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException(
                    "Entity type must not be null or blank"
            );
        }

        return notificationRepository
                .findByEntityIdAndEntityType(
                        entityId,
                        entityType
                );
    }

    /**
     * Mark a notification as read.
     *
     * IMPORTANT:
     * Notification has a boolean field named isRead.
     * Lombok generates setRead(), not setIsRead().
     */
    @Transactional
    public Notification markAsRead(UUID id) {

        Notification notification =
                getNotificationById(id);

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());

            Notification updatedNotification =
                    notificationRepository.save(notification);

            log.info(
                    "Marked notification {} as read",
                    updatedNotification.getId()
            );

            return updatedNotification;
        }

        return notification;
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(UUID userId) {

        validateId(userId, "User ID");

        List<Notification> notifications =
                notificationRepository
                        .findByUserIdAndIsReadFalse(userId);

        if (notifications.isEmpty()) {
            return;
        }

        Instant now = Instant.now();

        for (Notification notification : notifications) {
            notification.setRead(true);
            notification.setReadAt(now);
        }

        notificationRepository.saveAll(notifications);

        log.info(
                "Marked all notifications as read for user {} ({} notifications)",
                userId,
                notifications.size()
        );
    }

    /**
     * Archive a notification.
     *
     * IMPORTANT:
     * Lombok generates setArchived(), not setIsArchived().
     */
    @Transactional
    public Notification archive(UUID id) {

        Notification notification =
                getNotificationById(id);

        if (!notification.isArchived()) {
            notification.setArchived(true);

            Notification updatedNotification =
                    notificationRepository.save(notification);

            log.info(
                    "Archived notification {}",
                    updatedNotification.getId()
            );

            return updatedNotification;
        }

        return notification;
    }

    /**
     * Delete a notification.
     *
     * Notification uses @SQLDelete, therefore the repository
     * delete operation performs a soft delete.
     */
    @Transactional
    public void deleteNotification(UUID id) {

        Notification notification =
                getNotificationById(id);

        notificationRepository.delete(notification);

        log.info(
                "Deleted notification {}",
                id
        );
    }

    /**
     * Get notifications with filtering and pagination.
     *
     * The existing repository already supports the complete
     * criteria query.
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByCriteria(
            UUID userId,
            String notificationType,
            Boolean isRead,
            Boolean isArchived,
            UUID entityId,
            String entityType,
            Instant startTime,
            Instant endTime) {

        validateOptionalTimeRange(
                startTime,
                endTime
        );

        return notificationRepository
                .findByCriteria(
                        userId,
                        notificationType,
                        isRead,
                        isArchived,
                        entityId,
                        entityType,
                        startTime,
                        endTime,
                        PageRequest.of(
                                0,
                                Integer.MAX_VALUE
                        )
                )
                .getContent();
    }

    /**
     * Get count of unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {

        validateId(userId, "User ID");

        return notificationRepository
                .countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Validate UUID.
     */
    private void validateId(
            UUID id,
            String fieldName) {

        if (id == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null"
            );
        }
    }

    /**
     * Validate a required timestamp range.
     */
    private void validateTimeRange(
            Instant startTime,
            Instant endTime) {

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(
                    "Start time must not be after end time"
            );
        }
    }

    /**
     * Validate an optional timestamp range.
     */
    private void validateOptionalTimeRange(
            Instant startTime,
            Instant endTime) {

        if (startTime != null
                && endTime != null
                && startTime.isAfter(endTime)) {

            throw new IllegalArgumentException(
                    "Start time must not be after end time"
            );
        }
    }
}
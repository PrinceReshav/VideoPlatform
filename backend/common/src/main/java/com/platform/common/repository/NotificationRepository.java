package com.platform.common.repository;

import com.platform.common.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Notification entity.
 *
 * Feature 17: Notification Center enhancement.
 */
@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    /**
     * Find notifications by user ID.
     */
    List<Notification> findByUserId(
            UUID userId
    );

    /**
     * Find unread notifications by user ID.
     */
    List<Notification> findByUserIdAndIsReadFalse(
            UUID userId
    );

    /**
     * Count unread notifications by user ID.
     *
     * Required by NotificationService#getUnreadCount().
     */
    long countByUserIdAndIsReadFalse(
            UUID userId
    );

    /**
     * Find notifications by user ID and notification type.
     */
    List<Notification> findByUserIdAndNotificationType(
            UUID userId,
            String notificationType
    );

    /**
     * Find notifications by user ID and timestamp range.
     */
    List<Notification> findByUserIdAndCreatedAtBetween(
            UUID userId,
            Instant startTime,
            Instant endTime
    );

    /**
     * Find unread notifications by user ID and timestamp range.
     */
    List<Notification>
    findByUserIdAndIsReadFalseAndCreatedAtBetween(
            UUID userId,
            Instant startTime,
            Instant endTime
    );

    /**
     * Find notifications by entity ID and entity type.
     */
    List<Notification> findByEntityIdAndEntityType(
            UUID entityId,
            String entityType
    );

    /**
     * Find notifications with pagination.
     */
    Page<Notification> findAllBy(
            Pageable pageable
    );

    /**
     * Find notifications by multiple criteria.
     */
    @Query("""
            SELECT n
            FROM Notification n
            WHERE
                (:userId IS NULL OR n.userId = :userId)
                AND
                (:notificationType IS NULL
                    OR n.notificationType = :notificationType)
                AND
                (:isRead IS NULL
                    OR n.isRead = :isRead)
                AND
                (:isArchived IS NULL
                    OR n.isArchived = :isArchived)
                AND
                (:entityId IS NULL
                    OR n.entityId = :entityId)
                AND
                (:entityType IS NULL
                    OR n.entityType = :entityType)
                AND
                (:startTime IS NULL
                    OR n.createdAt >= :startTime)
                AND
                (:endTime IS NULL
                    OR n.createdAt <= :endTime)
            """)
    Page<Notification> findByCriteria(
            @Param("userId")
            UUID userId,

            @Param("notificationType")
            String notificationType,

            @Param("isRead")
            Boolean isRead,

            @Param("isArchived")
            Boolean isArchived,

            @Param("entityId")
            UUID entityId,

            @Param("entityType")
            String entityType,

            @Param("startTime")
            Instant startTime,

            @Param("endTime")
            Instant endTime,

            Pageable pageable
    );
}
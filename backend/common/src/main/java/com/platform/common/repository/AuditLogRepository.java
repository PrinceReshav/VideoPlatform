package com.platform.common.repository;

import com.platform.common.entity.AuditLog;
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
 * Repository for AuditLog entity (Feature 38: Security & Audit Logs)
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /** Find audit logs by user ID */
    List<AuditLog> findByUserId(UUID userId);

    /** Find audit logs by entity type and entity ID */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    /** Find audit logs by action type */
    List<AuditLog> findByActionType(String actionType);

    /** Find audit logs by timestamp range */
    List<AuditLog> findByTimestampBetween(Instant startTime, Instant endTime);

    /** Find audit logs by user ID and timestamp range */
    List<AuditLog> findByUserIdAndTimestampBetween(UUID userId, Instant startTime, Instant endTime);

    /** Find audit logs with pagination */
    Page<AuditLog> findAllBy(Pageable pageable);

    /** Find audit logs by multiple criteria */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:actionType IS NULL OR a.actionType = :actionType) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
            "(:endTime IS NULL OR a.timestamp <= :endTime)")
    Page<AuditLog> findByCriteria(
            @Param("userId") UUID userId,
            @Param("actionType") String actionType,
            @Param("entityType") String entityType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
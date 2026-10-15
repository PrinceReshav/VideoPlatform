package com.platform.common.repository;

import com.platform.common.entity.Report;
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
 * Repository for Report entity (Feature 18: Reporting/Strikes/Trust System)
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /** Find reports by reporter ID */
    List<Report> findByReporterId(UUID reporterId);

    /** Find reports by target ID and target type */
    List<Report> findByTargetIdAndTargetType(UUID targetId, String targetType);

    /** Find reports by reason */
    List<Report> findByReason(String reason);

    /** Find reports by status */
    List<Report> findByStatus(String status);

    /** Find reports by timestamp range */
    List<Report> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /** Find reports by reporter ID and timestamp range */
    List<Report> findByReporterIdAndCreatedAtBetween(UUID reporterId, Instant startTime, Instant endTime);

    /** Find reports by target ID, target type, and timestamp range */
    List<Report> findByTargetIdAndTargetTypeAndCreatedAtBetween(UUID targetId, String targetType, Instant startTime, Instant endTime);

    /** Find reports with pagination */
    Page<Report> findAllBy(Pageable pageable);

    /** Find reports by multiple criteria */
    @Query("SELECT r FROM Report r WHERE " +
            "(:reporterId IS NULL OR r.reporterId = :reporterId) AND " +
            "(:targetId IS NULL OR r.targetId = :targetId) AND " +
            "(:targetType IS NULL OR r.targetType = :targetType) AND " +
            "(:reason IS NULL OR r.reason = :reason) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:startTime IS NULL OR r.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR r.createdAt <= :endTime)")
    Page<Report> findByCriteria(
            @Param("reporterId") UUID reporterId,
            @Param("targetId") UUID targetId,
            @Param("targetType") String targetType,
            @Param("reason") String reason,
            @Param("status") String status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
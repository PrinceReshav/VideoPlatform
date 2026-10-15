package com.platform.common.repository;

import com.platform.common.entity.Strike;
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
 * Repository for Strike entity (Feature 18: Reporting/Strikes/Trust System)
 */
@Repository
public interface StrikeRepository extends JpaRepository<Strike, UUID> {

    /** Find strikes by target ID and target type */
    List<Strike> findByTargetIdAndTargetType(UUID targetId, String targetType);

    /** Find active strikes by target ID and target type */
    List<Strike> findByTargetIdAndTargetTypeAndIsActiveTrue(UUID targetId, String targetType);

    /** Find strikes by reason */
    List<Strike> findByReason(String reason);

    /** Find strikes by timestamp range */
    List<Strike> findByIssuedAtBetween(Instant startTime, Instant endTime);

    /** Find strikes by target ID, target type, and timestamp range */
    List<Strike> findByTargetIdAndTargetTypeAndIssuedAtBetween(UUID targetId, String targetType, Instant startTime, Instant endTime);

    /** Find strikes with pagination */
    Page<Strike> findAllBy(Pageable pageable);

    /** Find strikes by multiple criteria */
    @Query("SELECT s FROM Strike s WHERE " +
            "(:targetId IS NULL OR s.targetId = :targetId) AND " +
            "(:targetType IS NULL OR s.targetType = :targetType) AND " +
            "(:reason IS NULL OR s.reason = :reason) AND " +
            "(:isActive IS NULL OR s.isActive = :isActive) AND " +
            "(:issuedBy IS NULL OR s.issuedBy = :issuedBy) AND " +
            "(:reportId IS NULL OR s.reportId = :reportId) AND " +
            "(:startTime IS NULL OR s.issuedAt >= :startTime) AND " +
            "(:endTime IS NULL OR s.issuedAt <= :endTime)")
    Page<Strike> findByCriteria(
            @Param("targetId") UUID targetId,
            @Param("targetType") String targetType,
            @Param("reason") String reason,
            @Param("isActive") Boolean isActive,
            @Param("issuedBy") UUID issuedBy,
            @Param("reportId") UUID reportId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
package com.platform.common.repository;

import com.platform.common.entity.CopyrightDispute;
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
 * Repository for CopyrightDispute entity (Feature 3: Copyright Detection & Claims System)
 */
@Repository
public interface CopyrightDisputeRepository extends JpaRepository<CopyrightDispute, UUID> {

    /** Find copyright disputes by copyright claim ID */
    List<CopyrightDispute> findByCopyrightClaimId(UUID copyrightClaimId);

    /** Find copyright disputes by disputant ID */
    List<CopyrightDispute> findByDisputantId(UUID disputantId);

    /** Find copyright disputes by status */
    List<CopyrightDispute> findByStatus(String status);

    /** Find copyright disputes by dispute type */
    List<CopyrightDispute> findByDisputeType(String disputeType);

    /** Find copyright disputes by timestamp range */
    List<CopyrightDispute> findByFiledAtBetween(Instant startTime, Instant endTime);

    /** Find copyright disputes by copyright claim ID and status */
    List<CopyrightDispute> findByCopyrightClaimIdAndStatus(UUID copyrightClaimId, String status);

    /** Find copyright disputes with pagination */
    Page<CopyrightDispute> findAllBy(Pageable pageable);

    /** Find copyright disputes by multiple criteria */
    @Query("SELECT d FROM CopyrightDispute d WHERE " +
            "(:copyrightClaimId IS NULL OR d.copyrightClaimId = :copyrightClaimId) AND " +
            "(:disputantId IS NULL OR d.disputantId = :disputantId) AND " +
            "(:status IS NULL OR d.status = :status) AND " +
            "(:disputeType IS NULL OR d.disputeType = :disputeType) AND " +
            "(:startTime IS NULL OR d.filedAt >= :startTime) AND " +
            "(:endTime IS NULL OR d.filedAt <= :endTime)")
    Page<CopyrightDispute> findByCriteria(
            @Param("copyrightClaimId") UUID copyrightClaimId,
            @Param("disputantId") UUID disputantId,
            @Param("status") String status,
            @Param("disputeType") String disputeType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
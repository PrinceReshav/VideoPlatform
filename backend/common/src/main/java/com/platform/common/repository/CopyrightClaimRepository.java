package com.platform.common.repository;

import com.platform.common.entity.CopyrightClaim;
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
 * Repository for CopyrightClaim entity (Feature 3: Copyright Detection & Claims System)
 */
@Repository
public interface CopyrightClaimRepository extends JpaRepository<CopyrightClaim, UUID> {

    /** Find copyright claims by video ID */
    List<CopyrightClaim> findByVideoId(UUID videoId);

    /** Find copyright claims by claimant ID */
    List<CopyrightClaim> findByClaimantId(UUID claimantId);

    /** Find copyright claims by rights owner ID */
    List<CopyrightClaim> findByRightsOwnerId(UUID rightsOwnerId);

    /** Find copyright claims by status */
    List<CopyrightClaim> findByStatus(String status);

    /** Find copyright claims by infringement type */
    List<CopyrightClaim> findByInfringementType(String infringementType);

    /** Find copyright claims by timestamp range */
    List<CopyrightClaim> findByFiledAtBetween(Instant startTime, Instant endTime);

    /** Find copyright claims by video ID and status */
    List<CopyrightClaim> findByVideoIdAndStatus(UUID videoId, String status);

    /** Find copyright claims with pagination */
    Page<CopyrightClaim> findAllBy(Pageable pageable);

    /** Find copyright claims by multiple criteria */
    @Query("SELECT c FROM CopyrightClaim c WHERE " +
            "(:videoId IS NULL OR c.videoId = :videoId) AND " +
            "(:claimantId IS NULL OR c.claimantId = :claimantId) AND " +
            "(:rightsOwnerId IS NULL OR c.rightsOwnerId = :rightsOwnerId) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:infringementType IS NULL OR c.infringementType = :infringementType) AND " +
            "(:startTime IS NULL OR c.filedAt >= :startTime) AND " +
            "(:endTime IS NULL OR c.filedAt <= :endTime)")
    Page<CopyrightClaim> findByCriteria(
            @Param("videoId") UUID videoId,
            @Param("claimantId") UUID claimantId,
            @Param("rightsOwnerId") UUID rightsOwnerId,
            @Param("status") String status,
            @Param("infringementType") String infringementType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
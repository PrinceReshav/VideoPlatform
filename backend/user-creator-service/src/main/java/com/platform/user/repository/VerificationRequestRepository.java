package com.platform.user.repository;

import com.platform.user.entity.VerificationRequest;
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
 * Repository for VerificationRequest entity (Feature 19: Creator Verification)
 */
@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

    /** Find verification requests by user ID */
    List<VerificationRequest> findByUserId(UUID userId);

    /** Find verification requests by status */
    List<VerificationRequest> findByStatus(String status);

    /** Find verification requests by request type */
    List<VerificationRequest> findByRequestType(String requestType);

    /** Find verification requests by requested tier */
    List<VerificationRequest> findByRequestedTier(String requestedTier);

    /** Find verification requests by user ID and status */
    List<VerificationRequest> findByUserIdAndStatus(UUID userId, String status);

    /** Find verification requests with pagination */
    Page<VerificationRequest> findAllBy(Pageable pageable);

    /** Find verification requests by multiple criteria */
    @Query("SELECT v FROM VerificationRequest v WHERE " +
            "(:userId IS NULL OR v.userId = :userId) AND " +
            "(:requestType IS NULL OR v.requestType = :requestType) AND " +
            "(:status IS NULL OR v.status = :status) AND " +
            "(:requestedTier IS NULL OR v.requestedTier = :requestedTier) AND " +
            "(:reviewedBy IS NULL OR v.reviewedBy = :reviewedBy) AND " +
            "(:processed IS NULL OR v.processed = :processed) AND " +
            "(:startTime IS NULL OR v.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR v.createdAt <= :endTime)")
    Page<VerificationRequest> findByCriteria(
            @Param("userId") UUID userId,
            @Param("requestType") String requestType,
            @Param("status") String status,
            @Param("requestedTier") String requestedTier,
            @Param("reviewedBy") UUID reviewedBy,
            @Param("processed") Boolean processed,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
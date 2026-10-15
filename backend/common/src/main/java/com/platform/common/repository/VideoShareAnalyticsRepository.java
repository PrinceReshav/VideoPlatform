package com.platform.common.repository;

import com.platform.common.entity.VideoShareAnalytics;
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
 * Repository for VideoShareAnalytics entity (Feature 13: Privacy & Sharing Controls)
 */
@Repository
public interface VideoShareAnalyticsRepository extends JpaRepository<VideoShareAnalytics, UUID> {

    /** Find analytics by share link ID */
    List<VideoShareAnalytics> findByShareLinkId(UUID shareLinkId);

    /** Find analytics by video ID */
    List<VideoShareAnalytics> findByVideoId(UUID videoId);

    /** Find analytics by creator ID */
    List<VideoShareAnalytics> findByCreatedBy(UUID createdBy);

    /** Find analytics by timestamp range */
    List<VideoShareAnalytics> findByAccessedAtBetween(Instant startTime, Instant endTime);

    /** Find analytics by share link ID and timestamp range */
    List<VideoShareAnalytics> findByShareLinkIdAndAccessedAtBetween(UUID shareLinkId, Instant startTime, Instant endTime);

    /** Find analytics with pagination */
    Page<VideoShareAnalytics> findAllBy(Pageable pageable);

    /** Find analytics by multiple criteria */
    @Query("SELECT sa FROM VideoShareAnalytics sa WHERE " +
            "(:shareLinkId IS NULL OR sa.shareLinkId = :shareLinkId) AND " +
            "(:videoId IS NULL OR sa.videoId = :videoId) AND " +
            "(:createdBy IS NULL OR sa.createdBy = :createdBy) AND " +
            "(:startTime IS NULL OR sa.accessedAt >= :startTime) AND " +
            "(:endTime IS NULL OR sa.accessedAt <= :endTime)")
    Page<VideoShareAnalytics> findByCriteria(
            @Param("shareLinkId") UUID shareLinkId,
            @Param("videoId") UUID videoId,
            @Param("createdBy") UUID createdBy,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
package com.platform.common.repository;

import com.platform.common.entity.VideoShareLink;
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
 * Repository for VideoShareLink entity (Feature 13: Privacy & Sharing Controls)
 */
@Repository
public interface VideoShareLinkRepository extends JpaRepository<VideoShareLink, UUID> {

    /** Find share links by video ID */
    List<VideoShareLink> findByVideoId(UUID videoId);

    /** Find share links by creator ID */
    List<VideoShareLink> findByCreatedBy(UUID createdBy);

    /** Find share links by share token */
    VideoShareLink findByShareToken(String shareToken);

    /** Find share links by timestamp range */
    List<VideoShareLink> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /** Find share links by video ID and timestamp range */
    List<VideoShareLink> findByVideoIdAndCreatedAtBetween(UUID videoId, Instant startTime, Instant endTime);

    /** Find share links with pagination */
    Page<VideoShareLink> findAllBy(Pageable pageable);

    /** Find share links by multiple criteria */
    @Query("SELECT sl FROM VideoShareLink sl WHERE " +
            "(:videoId IS NULL OR sl.videoId = :videoId) AND " +
            "(:createdBy IS NULL OR sl.createdBy = :createdBy) AND " +
            "(:shareToken IS NULL OR sl.shareToken = :shareToken) AND " +
            "(:startTime IS NULL OR sl.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR sl.createdAt <= :endTime)")
    Page<VideoShareLink> findByCriteria(
            @Param("videoId") UUID videoId,
            @Param("createdBy") UUID createdBy,
            @Param("shareToken") String shareToken,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
package com.platform.video.repository;

import com.platform.video.entity.VideoVersion;
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
 * Repository for VideoVersion entity (Feature 21: Video Versioning)
 */
@Repository
public interface VideoVersionRepository extends JpaRepository<VideoVersion, UUID> {

    /** Find video versions by video ID */
    List<VideoVersion> findByVideoId(UUID videoId);

    /** Find video versions by video ID ordered by version number descending */
    List<VideoVersion> findByVideoIdOrderByVersionNumberDesc(UUID videoId);

    /** Find current video version by video ID */
    VideoVersion findByVideoIdAndIsCurrentTrue(UUID videoId);

    /** Find video versions with pagination */
    Page<VideoVersion> findAllBy(Pageable pageable);

    /** Find video versions by multiple criteria */
    @Query("SELECT v FROM VideoVersion v WHERE " +
            "(:videoId IS NULL OR v.videoId = :videoId) AND " +
            "(:versionNumber IS NULL OR v.versionNumber = :versionNumber) AND " +
            "(:title IS NULL OR v.title = :title) AND " +
            "(:changedBy IS NULL OR v.changedBy = :changedBy) AND " +
            "(:isCurrent IS NULL OR v.isCurrent = :isCurrent) AND " +
            "(:startTime IS NULL OR v.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR v.createdAt <= :endTime)")
    Page<VideoVersion> findByCriteria(
            @Param("videoId") UUID videoId,
            @Param("versionNumber") Integer versionNumber,
            @Param("title") String title,
            @Param("changedBy") UUID changedBy,
            @Param("isCurrent") Boolean isCurrent,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
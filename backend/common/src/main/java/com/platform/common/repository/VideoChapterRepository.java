package com.platform.common.repository;

import com.platform.common.entity.VideoChapter;
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
 * Repository for VideoChapter entity (Feature 12: Video Chapters)
 */
@Repository
public interface VideoChapterRepository extends JpaRepository<VideoChapter, UUID> {

    /** Find video chapters by video ID */
    List<VideoChapter> findByVideoId(UUID videoId);

    /** Find video chapters by video ID, ordered by position */
    List<VideoChapter> findByVideoIdOrderByPositionAsc(UUID videoId);

    /** Find video chapters by timestamp range */
    List<VideoChapter> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /** Find video chapters by video ID and timestamp range */
    List<VideoChapter> findByVideoIdAndCreatedAtBetween(UUID videoId, Instant startTime, Instant endTime);

    /** Find video chapters with pagination */
    Page<VideoChapter> findAllBy(Pageable pageable);

    /** Find video chapters by multiple criteria */
    @Query("SELECT vc FROM VideoChapter vc WHERE " +
            "(:videoId IS NULL OR vc.video.id = :videoId) AND " +
            "(:startTime IS NULL OR vc.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR vc.createdAt <= :endTime)")
    Page<VideoChapter> findByCriteria(
            @Param("videoId") UUID videoId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
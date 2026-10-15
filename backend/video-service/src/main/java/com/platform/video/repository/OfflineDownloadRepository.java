package com.platform.video.repository;

import com.platform.video.entity.OfflineDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.platform.video.entity.OfflineDownload;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for OfflineDownload entities (Feature 34: Offline Downloads with DRM)
 */
@Repository
public interface OfflineDownloadRepository extends JpaRepository<OfflineDownload, UUID> {

    /** Find offline downloads by user ID */
    List<OfflineDownload> findByUserId(UUID userId);

    /** Find offline downloads by video ID */
    List<OfflineDownload> findByVideoId(UUID videoId);

    /** Find offline downloads by status */
    List<OfflineDownload> findByStatus(OfflineDownload.OfflineDownloadStatus status);

    /** Find active offline downloads for a user */
    List<OfflineDownload> findByUserIdAndStatus(UUID userId, OfflineDownload.OfflineDownloadStatus status);

    /** Find offline downloads that have expired */

    @Query("""
    SELECT o
    FROM OfflineDownload o
    WHERE o.expiresAt < :now
      AND o.status = :status
    """)
    List<OfflineDownload> findExpiredDownloads(
            @Param("now") Instant now,
            @Param("status") OfflineDownload.OfflineDownloadStatus status
    );


    /** Find offline downloads by user and video */
    OfflineDownload findByUserIdAndVideoId(UUID userId, UUID videoId);


    long countByStatus(OfflineDownload.OfflineDownloadStatus status);

    long countByCreatedAtAfter(Instant createdAt);
}
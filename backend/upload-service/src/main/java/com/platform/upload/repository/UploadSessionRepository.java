package com.platform.upload.repository;

import com.platform.upload.entity.UploadChunk;
import com.platform.upload.entity.UploadSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    List<UploadSession> findByUserId(UUID userId);

    Optional<UploadSession> findByVideoId(UUID videoId);

    List<UploadSession> findByStatus(UploadSession.Status status);

    @Query("SELECT us FROM UploadSession us WHERE us.status IN (:statuses) AND us.expiresAt > :now")
    List<UploadSession> findActiveSessions(@Param("statuses") List<UploadSession.Status> statuses, @Param("now") java.time.Instant now);

    @Query("SELECT us FROM UploadSession us WHERE us.expiresAt < :now AND us.status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED')")
    List<UploadSession> findExpiredSessions(@Param("now") java.time.Instant now);

    Page<UploadSession> findByUserId(UUID userId, Pageable pageable);
}
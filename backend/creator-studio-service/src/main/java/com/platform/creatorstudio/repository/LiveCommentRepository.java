package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.LiveComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LiveComment entity.
 * Provides data access operations for live session comments.
 */
@Repository
public interface LiveCommentRepository extends JpaRepository<LiveComment, UUID> {

    /** Find comments by live session ID */
    List<LiveComment> findBySessionId(UUID sessionId);

    /** Find approved comments by live session ID */
    List<LiveComment> findBySessionIdAndIsApprovedTrue(UUID sessionId);

    /** Find comments by user ID */
    List<LiveComment> findByUserId(UUID userId);

    /** Find comments by user ID and session ID */
    List<LiveComment> findByUserIdAndSessionId(UUID userId, UUID sessionId);

    /** Find recent comments for a session */
    @Query("SELECT c FROM LiveComment c WHERE " +
           "c.sessionId = :sessionId AND " +
           "c.isApproved = true AND c.isDeleted = false " +
           "ORDER BY c.createdAt DESC")
    List<LiveComment> findRecentApprovedCommentsBySession(
            @Param("sessionId") UUID sessionId);

    /** Count comments by session */
    @Query("SELECT COUNT(c) FROM LiveComment c WHERE c.sessionId = :sessionId AND c.isApproved = true")
    Long countApprovedCommentsBySession(@Param("sessionId") UUID sessionId);

    /** Find comments that need moderation */
    List<LiveComment> findByIsApprovedFalseAndIsDeletedFalse();

    /** Delete comments older than a certain date (for cleanup) */
    @Query("DELETE FROM LiveComment c WHERE c.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") java.time.Instant cutoff);
}
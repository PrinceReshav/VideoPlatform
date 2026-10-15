package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.StudioLiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StudioLiveSession entity.
 * Provides data access operations for studio live sessions.
 */
@Repository
public interface StudioLiveSessionRepository extends JpaRepository<StudioLiveSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StudioLiveSession s WHERE s.id = :id")
    Optional<StudioLiveSession> findByIdForUpdate(@Param("id") UUID id);

    /** Find live sessions by studio ID */
    List<StudioLiveSession> findByStudioId(UUID studioId);

    /** Find upcoming live sessions by studio ID */
    List<StudioLiveSession> findByStudioIdAndStatus(UUID studioId, com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus status);

    /** Find live sessions by status */
    List<StudioLiveSession> findByStatus(com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus status);

    /** Find live sessions by ID and studio ID */
    Optional<StudioLiveSession> findByIdAndStudioId(UUID id, UUID studioId);

    /** Find upcoming scheduled sessions */
    @Query("SELECT s FROM StudioLiveSession s WHERE " +
           "s.status = :status AND " +
           "s.scheduledStartTime > :now " +
           "ORDER BY s.scheduledStartTime ASC")
    List<StudioLiveSession> findUpcomingSessions(
            @Param("status") com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus status,
            @Param("now") java.time.Instant now);

    /** Find recently ended sessions (for replay availability) */
    @Query("SELECT s FROM StudioLiveSession s WHERE " +
           "s.status IN (:ended, :completed) AND " +
           "s.actualEndTime > :cutoff " +
           "ORDER BY s.actualEndTime DESC")
    List<StudioLiveSession> findRecentlyEndedSessions(
            @Param("ended") com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus ended,
            @Param("completed") com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus completed,
            @Param("cutoff") java.time.Instant cutoff);

    /** Count live sessions by studio and status */
    @Query("SELECT COUNT(s) FROM StudioLiveSession s WHERE s.studioId = :studioId AND s.status = :status")
    Long countByStudioIdAndStatus(@Param("studioId") UUID studioId,
                                  @Param("status") com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus status);
}
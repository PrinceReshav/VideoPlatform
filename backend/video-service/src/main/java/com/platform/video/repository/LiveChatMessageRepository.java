package com.platform.video.repository;

import com.platform.video.entity.LiveChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LiveChatMessage entities (Feature 31: Live Streaming Platform)
 */
@Repository
public interface LiveChatMessageRepository extends JpaRepository<LiveChatMessage, UUID> {

    /** Find chat messages by live stream ID */
    List<LiveChatMessage> findByLiveStreamId(UUID liveStreamId);

    /** Find chat messages by live stream ID ordered by creation time (newest first) */
    @Query("SELECT l FROM LiveChatMessage l WHERE l.liveStreamId = :liveStreamId AND l.isDeleted = false ORDER BY l.createdAt DESC")
    List<LiveChatMessage> findByLiveStreamIdOrderByCreatedAtDesc(@Param("liveStreamId") UUID liveStreamId);

    /** Find chat messages by live stream ID after a given time */
    @Query("SELECT l FROM LiveChatMessage l WHERE l.liveStreamId = :liveStreamId AND l.isDeleted = false AND l.createdAt > :afterTime ORDER BY l.createdAt ASC")
    List<LiveChatMessage> findByLiveStreamIdAfter(@Param("liveStreamId") UUID liveStreamId, @Param("afterTime") Instant afterTime);

    /** Find chat messages by sender ID */
    List<LiveChatMessage> findBySenderId(UUID senderId);
}
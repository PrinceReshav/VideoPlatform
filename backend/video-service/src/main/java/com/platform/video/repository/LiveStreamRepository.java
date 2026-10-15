package com.platform.video.repository;

import com.platform.video.entity.LiveStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.platform.video.entity.LiveStream;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

import java.util.List;
import java.util.UUID;

/**
 * Repository for LiveStream entities (Feature 31: Live Streaming Platform)
 */
@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, UUID> {

    /** Find live streams by creator ID */
    List<LiveStream> findByCreatorId(UUID creatorId);

    /** Find live streams by channel ID */
    List<LiveStream> findByChannelId(UUID channelId);

    /** Find live streams by status */
    List<LiveStream> findByStatus(LiveStream.LiveStreamStatus status);

    /** Find live streams that are currently online */

    @Query("SELECT l FROM LiveStream l WHERE l.status = :status")
    List<LiveStream> findOnlineStreams(
            @Param("status") LiveStream.LiveStreamStatus status
    );


    /** Find live stream by stream key */
    LiveStream findByStreamKey(String streamKey);

    /** Find live streams that started after a given time */
    List<LiveStream> findByStartedAtAfter(Instant startedAt);

    /** Find live streams that ended before a given time */
    List<LiveStream> findByEndedAtBefore(Instant endedAt);

    long countByStatus(LiveStream.LiveStreamStatus status);

    long countByStartedAtAfter(Instant startedAt);


}
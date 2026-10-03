package com.platform.engagement.repository;

import com.platform.engagement.entity.View;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViewRepository extends JpaRepository<View, UUID> {

    long countByVideoId(UUID videoId);

    long countByVideoIdAndCreatedAtAfter(UUID videoId, Instant after);

    List<View> findByVideoId(UUID videoId);

    List<View> findByUserId(UUID userId);

    Optional<View> findByVideoIdAndUserIdAndSessionId(UUID videoId, UUID userId, String sessionId);
}
package com.platform.engagement.repository;

import com.platform.engagement.entity.WatchHistory;
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
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, UUID> {

    Optional<WatchHistory> findByUserIdAndVideoId(UUID userId, UUID videoId);

    Page<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(UUID userId, Pageable pageable);

    List<WatchHistory> findByUserIdAndProgressPercentGreaterThan(UUID userId, int progress);

    List<WatchHistory> findByUserIdAndCompletedTrue(UUID userId);

    long countByUserId(UUID userId);
}
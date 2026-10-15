package com.platform.engagement.repository;

import com.platform.engagement.entity.SavedVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SavedVideo entity (Feature 24: Watch Later)
 */
@Repository
public interface SavedVideoRepository extends JpaRepository<SavedVideo, UUID> {

    /** Find saved videos by user ID */
    List<SavedVideo> findByUserId(UUID userId);

    /** Find saved videos by video ID */
    List<SavedVideo> findByVideoId(UUID videoId);

    /** Check if a video is saved by a user */
    Optional<SavedVideo> findByUserIdAndVideoId(UUID userId, UUID videoId);

    /** Find saved videos with pagination */
    Page<SavedVideo> findByUserId(UUID userId, Pageable pageable);
}
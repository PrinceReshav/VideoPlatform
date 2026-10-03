package com.platform.engagement.repository;

import com.platform.engagement.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    Optional<Like> findByUserIdAndVideoId(UUID userId, UUID videoId);

    long countByVideoId(UUID videoId);

    List<Like> findByUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.userId = :userId AND l.videoId = :videoId")
    int deleteByUserIdAndVideoId(@Param("userId") UUID userId, @Param("videoId") UUID videoId);

    boolean existsByUserIdAndVideoId(UUID userId, UUID videoId);
}
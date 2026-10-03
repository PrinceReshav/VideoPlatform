package com.platform.playlist.repository;

import com.platform.playlist.entity.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, UUID> {

    List<PlaylistItem> findByPlaylistIdOrderByPositionAsc(UUID playlistId);

    Optional<PlaylistItem> findByPlaylistIdAndVideoId(UUID playlistId, UUID videoId);

    long countByPlaylistId(UUID playlistId);

    @Modifying
    @Transactional
    @Query("UPDATE PlaylistItem pi SET pi.position = pi.position + 1 WHERE pi.playlistId = :playlistId AND pi.position >= :position")
    int shiftPositionsForward(@Param("playlistId") UUID playlistId, @Param("position") int position);

    @Modifying
    @Transactional
    @Query("UPDATE PlaylistItem pi SET pi.position = pi.position - 1 WHERE pi.playlistId = :playlistId AND pi.position > :position")
    int shiftPositionsBackward(@Param("playlistId") UUID playlistId, @Param("position") int position);

    void deleteByPlaylistId(UUID playlistId);
}
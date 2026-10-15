package com.platform.playlist.repository;

import com.platform.playlist.entity.PlaylistCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PlaylistCollaborator entities (Feature 25: Advanced Playlists)
 */
@Repository
public interface PlaylistCollaboratorRepository extends JpaRepository<PlaylistCollaborator, UUID> {

    /** Find collaborators by playlist ID */
    List<PlaylistCollaborator> findByPlaylistId(UUID playlistId);

    /** Find collaborators by user ID */
    List<PlaylistCollaborator> findByUserId(UUID userId);

    /** Find collaborator by playlist and user */
    Optional<PlaylistCollaborator> findByPlaylistIdAndUserId(
            UUID playlistId,
            UUID userId
    );

    /** Check if user is a collaborator on a playlist */
    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END " +
           "FROM PlaylistCollaborator pc WHERE pc.playlistId = :playlistId AND pc.userId = :userId")
    boolean existsByPlaylistIdAndUserId(@Param("playlistId") UUID playlistId, @Param("userId") UUID userId);
}
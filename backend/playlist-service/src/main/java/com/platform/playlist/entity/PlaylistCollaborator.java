package com.platform.playlist.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for playlist collaborators (Feature 25: Advanced Playlists)
 * Tracks which users have access to collaborate on playlists
 */
@Entity
@Table(
        name = "playlist_collaborators",
        schema = "playlist_schema",
        indexes = {
                @Index(
                        name = "idx_playlist_collaborators_playlist",
                        columnList = "playlist_id"
                ),
                @Index(
                        name = "idx_playlist_collaborators_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_playlist_collaborators_playlist_user",
                        columnList = "playlist_id, user_id",
                        unique = true
                )
        }
)
@Getter
@Setter
public class PlaylistCollaborator extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The playlist this collaborator has access to */
    @Column(name = "playlist_id", nullable = false)
    private UUID playlistId;

    /** The user who is a collaborator on the playlist */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The permission level of the collaborator */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    private PermissionLevel permissionLevel = PermissionLevel.CAN_EDIT;

    /** When the collaborator was added */
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    public enum PermissionLevel {
        CAN_VIEW,   // Can view the playlist but not modify
        CAN_EDIT,   // Can edit the playlist (add/remove/reorder videos)
        CAN_MANAGE  // Can edit playlist and manage collaborators
    }
}
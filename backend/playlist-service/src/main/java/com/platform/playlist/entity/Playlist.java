package com.platform.playlist.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for playlists (Feature 25: Advanced Playlists)
 * Extended to support collaborative playlists, rules, and sharing
 */
@Entity
@Table(name = "playlists", schema = "playlist_schema",
        indexes = {
                @Index(name = "idx_playlists_user", columnList = "user_id"),
                @Index(name = "idx_playlists_visibility", columnList = "visibility"),
                @Index(name = "idx_playlists_created_at", columnList = "created_at"),
                @Index(name = "idx_playlists_collaborative", columnList = "is_collaborative")
        })
@Getter
@Setter
public class Playlist extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId; // Owner/user who created the playlist

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

    /** Whether this is a collaborative playlist (multiple users can edit) */
    @Column(name = "is_collaborative", nullable = false)
    private boolean isCollaborative = false;

    /** Whether auto-add based on rules is enabled */
    @Column(name = "auto_add_enabled", nullable = false)
    private boolean autoAddEnabled = false;

    /** Rules for auto-adding videos (JSON format) */
    @Column(name = "auto_add_rules", length = 1000)
    private String autoAddRules;

    /** When the playlist was last modified (by any collaborator) */
    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    /** Number of followers/subscribers to this playlist */
    @Column(name = "follower_count", nullable = false)
    private int followerCount = 0;

    @Column(name = "video_count", nullable = false)
    private int videoCount = 0;

    @Column(name = "total_duration_seconds", nullable = false)
    private long totalDurationSeconds = 0;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    public enum Visibility {
        PUBLIC, UNLISTED, PRIVATE
    }
}
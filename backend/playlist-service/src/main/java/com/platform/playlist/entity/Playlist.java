package com.platform.playlist.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "playlists", schema = "playlist_schema",
        indexes = {
                @Index(name = "idx_playlists_user", columnList = "user_id"),
                @Index(name = "idx_playlists_visibility", columnList = "visibility"),
                @Index(name = "idx_playlists_created_at", columnList = "created_at")
        })
@Getter
@Setter
public class Playlist extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

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
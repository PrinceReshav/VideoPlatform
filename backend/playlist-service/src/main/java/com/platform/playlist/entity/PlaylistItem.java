package com.platform.playlist.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "playlist_items", schema = "playlist_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_playlist_items_playlist_video", columnNames = {"playlist_id", "video_id"})
        },
        indexes = {
                @Index(name = "idx_playlist_items_playlist", columnList = "playlist_id"),
                @Index(name = "idx_playlist_items_video", columnList = "video_id"),
                @Index(name = "idx_playlist_items_position", columnList = "playlist_id, position")
        })
@Getter
@Setter
public class PlaylistItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "playlist_id", nullable = false)
    private UUID playlistId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();
}
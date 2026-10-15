package com.platform.playlist.dto;

import com.platform.playlist.entity.Playlist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for playlist responses (Feature 25: Advanced Playlists)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String description;
    private Playlist.Visibility visibility;
    /** Whether this is a collaborative playlist (multiple users can edit) */
    private boolean isCollaborative;
    /** Whether auto-add based on rules is enabled */
    private boolean autoAddEnabled;
    /** Rules for auto-adding videos (JSON format) */
    private String autoAddRules;
    /** When the playlist was last modified (by any collaborator) */
    private Instant lastModifiedAt;
    /** Number of followers/subscribers to this playlist */
    private int followerCount;
    private int videoCount;
    private long totalDurationSeconds;
    private String thumbnailUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public static PlaylistResponse fromEntity(Playlist playlist) {
        return PlaylistResponse.builder()
                .id(playlist.getId())
                .userId(playlist.getUserId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .visibility(playlist.getVisibility())
                .isCollaborative(playlist.isCollaborative())
                .autoAddEnabled(playlist.isAutoAddEnabled())
                .autoAddRules(playlist.getAutoAddRules())
                .lastModifiedAt(playlist.getLastModifiedAt())
                .followerCount(playlist.getFollowerCount())
                .videoCount(playlist.getVideoCount())
                .totalDurationSeconds(playlist.getTotalDurationSeconds())
                .thumbnailUrl(playlist.getThumbnailUrl())
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .build();
    }
}

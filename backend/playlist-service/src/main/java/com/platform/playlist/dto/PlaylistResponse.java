package com.platform.playlist.dto;

import com.platform.playlist.entity.Playlist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

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
    private int videoCount;
    private long totalDurationSeconds;
    private String thumbnailUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
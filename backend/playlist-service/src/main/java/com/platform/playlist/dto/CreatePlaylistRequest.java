package com.platform.playlist.dto;

import com.platform.playlist.entity.Playlist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating playlists (Feature 25: Advanced Playlists)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlaylistRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 200, message = "Name must be 1-200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Playlist.Visibility visibility = Playlist.Visibility.PRIVATE;

    /** Whether this is a collaborative playlist (multiple users can edit) */
    private boolean isCollaborative = false;

    /** Whether auto-add based on rules is enabled */
    private boolean autoAddEnabled = false;

    /** Rules for auto-adding videos (JSON format) */
    private String autoAddRules;
}
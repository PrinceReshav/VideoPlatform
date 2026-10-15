package com.platform.playlist.dto;

import com.platform.playlist.entity.Playlist;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for updating playlists (Feature 25: Advanced Playlists)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlaylistRequest {

    @Size(min = 1, max = 200, message = "Name must be 1-200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Playlist.Visibility visibility;

    /** Whether this is a collaborative playlist (multiple users can edit) */
    private Boolean isCollaborative;

    /** Whether auto-add based on rules is enabled */
    private Boolean autoAddEnabled;

    /** Rules for auto-adding videos (JSON format) */
    private String autoAddRules;
}
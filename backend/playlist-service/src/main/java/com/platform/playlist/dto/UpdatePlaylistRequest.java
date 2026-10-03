package com.platform.playlist.dto;

import com.platform.playlist.entity.Playlist;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
}
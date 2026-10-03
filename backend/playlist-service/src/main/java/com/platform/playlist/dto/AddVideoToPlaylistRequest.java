package com.platform.playlist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddVideoToPlaylistRequest {
    @NotNull(message = "Video ID is required")
    private UUID videoId;

    @Positive(message = "Position must be positive")
    private Integer position; // Optional: auto-append if null
}
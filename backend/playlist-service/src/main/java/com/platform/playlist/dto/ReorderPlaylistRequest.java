package com.platform.playlist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderPlaylistRequest {
    @NotNull(message = "Video IDs list is required")
    @Size(min = 1, message = "At least one video ID required")
    private List<UUID> videoIds;
}
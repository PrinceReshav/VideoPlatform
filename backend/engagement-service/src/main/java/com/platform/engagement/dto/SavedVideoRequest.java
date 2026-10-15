package com.platform.engagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for saving videos for later (Feature 24: Watch Later)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedVideoRequest {
    @NotNull(message = "Video ID is required")
    private UUID videoId;
}
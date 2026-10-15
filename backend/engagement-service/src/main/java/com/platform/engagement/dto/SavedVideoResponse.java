package com.platform.engagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for saved video responses (Feature 24: Watch Later)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedVideoResponse {
    private UUID id;
    private UUID userId;
    private UUID videoId;
    private Instant savedAt;
}
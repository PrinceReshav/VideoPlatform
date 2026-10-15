package com.platform.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for creating/updating live streams (Feature 31: Live Streaming Platform)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStreamRequest {

    /** The channel associated with this live stream */
    private UUID channelId;

    /** Title of the live stream */
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    /** Description of the live stream */
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /** Whether the live stream should be saved as VOD after ending */
    private boolean saveAsVod = true;
}
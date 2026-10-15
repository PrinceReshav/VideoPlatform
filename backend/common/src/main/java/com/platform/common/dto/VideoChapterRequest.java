package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for video chapter requests (Feature 12: Video Chapters)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoChapterRequest {

    /** The video this chapter belongs to */
    @NotNull
    private UUID videoId;

    /** The title of the chapter */
    @NotBlank
    @Size(max = 200)
    private String title;

    /** The start time of the chapter in seconds */
    @NotNull
    @Min(0)
    private Integer startTimeSeconds;

    /** The end time of the chapter in seconds */
    @Min(0)
    private Integer endTimeSeconds;

    /** A custom thumbnail URL for the chapter (optional) */
    @Size(max = 500)
    private String thumbnailUrl;

    /** Description or summary of the chapter */
    @Size(max = 1000)
    private String description;

    /** Order/position of the chapter in the video */
    @NotNull
    @Min(0)
    private Integer position;
}
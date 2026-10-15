package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for video chapter responses (Feature 12: Video Chapters)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoChapterResponse {

    /** The unique identifier of the video chapter */
    private UUID id;

    /** The video this chapter belongs to */
    private UUID videoId;

    /** The title of the chapter */
    private String title;

    /** The start time of the chapter in seconds */
    private Integer startTimeSeconds;

    /** The end time of the chapter in seconds */
    private Integer endTimeSeconds;

    /** A custom thumbnail URL for the chapter (optional) */
    private String thumbnailUrl;

    /** Description or summary of the chapter */
    private String description;

    /** Order/position of the chapter in the video */
    private Integer position;

    /** Timestamp when the chapter was created */
    private Instant createdAt;

    /** Timestamp when the chapter was last updated */
    private Instant updatedAt;
}
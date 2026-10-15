package com.platform.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for VideoVersion entity (Feature 21: Video Versioning)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoVersionDTO {

    /** Unique identifier */
    private UUID id;

    /** The video this version belongs to */
    private UUID videoId;

    /** Version number (1, 2, 3, ...) */
    private int versionNumber;

    /** Title of the video at this version */
    private String title;

    /** Description of the video at this version */
    private String description;

    /** Tags of the video at this version */
    private String tags;

    /** Category ID at this version */
    private UUID categoryId;

    /** Genre ID at this version */
    private UUID genreId;

    /** Language ID at this version */
    private UUID languageId;

    /** Visibility at this version */
    private String visibility;

    /** Processing state at this version */
    private String processingState;

    /** Publication state at this version */
    private String publicationState;

    /** Published at timestamp at this version */
    private Instant publishedAt;

    /** Duration in seconds at this version */
    private Integer durationSeconds;

    /** Thumbnail URL at this version */
    private String thumbnailUrl;

    /** Preview URL at this version */
    private String previewUrl;

    /** Age restricted flag at this version */
    private boolean ageRestricted;

    /** Allow comments flag at this version */
    private boolean allowComments;

    /** Allow embedding flag at this version */
    private boolean allowEmbedding;

    /** Change summary - what changed in this version */
    private String changeSummary;

    /** Changed by - user ID who made the change */
    private UUID changedBy;

    /** Whether this is the current version */
    private boolean isCurrent;

    /** Creation timestamp */
    private Instant createdAt;

    /** Last update timestamp */
    private Instant updatedAt;
}
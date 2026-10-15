package com.platform.video.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for video versioning (Feature 21: Video Versioning)
 * Tracks changes to videos over time
 */
@Entity
@Table(name = "video_versions", schema = "video_schema",
        indexes = {
                @Index(name = "idx_video_versions_video", columnList = "video_id"),
                @Index(name = "idx_video_versions_created", columnList = "created_at"),
                @Index(name = "idx_video_versions_version_number", columnList = "version_number")
        })
@Getter
@Setter
public class VideoVersion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The video this version belongs to */
    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    /** Version number (1, 2, 3, ...) */
    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    /** Title of the video at this version */
    @Column(name = "title", length = 200)
    private String title;

    /** Description of the video at this version */
    @Column(name = "description", length = 5000)
    private String description;

    /** Tags of the video at this version */
    @Column(name = "tags", length = 1000)
    private String tags;

    /** Category ID at this version */
    @Column(name = "category_id")
    private UUID categoryId;

    /** Genre ID at this version */
    @Column(name = "genre_id")
    private UUID genreId;

    /** Language ID at this version */
    @Column(name = "language_id")
    private UUID languageId;

    /** Visibility at this version */
    @Column(name = "visibility", length = 20)
    private String visibility;

    /** Processing state at this version */
    @Column(name = "processing_state", length = 20)
    private String processingState;

    /** Publication state at this version */
    @Column(name = "publication_state", length = 20)
    private String publicationState;

    /** Published at timestamp at this version */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** Duration in seconds at this version */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** Thumbnail URL at this version */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** Preview URL at this version */
    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    /** Age restricted flag at this version */
    @Column(name = "age_restricted")
    private boolean ageRestricted;

    /** Allow comments flag at this version */
    @Column(name = "allow_comments")
    private boolean allowComments;

    /** Allow embedding flag at this version */
    @Column(name = "allow_embedding")
    private boolean allowEmbedding;

    /** Change summary - what changed in this version */
    @Column(name = "change_summary", length = 1000)
    private String changeSummary;

    /** Changed by - user ID who made the change */
    @Column(name = "changed_by")
    private UUID changedBy;

    /** Whether this is the current version */
    @Column(name = "is_current", nullable = false)
    private boolean isCurrent = false;
}
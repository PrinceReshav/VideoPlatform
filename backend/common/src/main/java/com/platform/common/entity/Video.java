package com.platform.common.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "videos", schema = "video_schema",
        indexes = {
                @Index(name = "idx_videos_creator", columnList = "creator_id"),
                @Index(name = "idx_videos_channel", columnList = "channel_id"),
                @Index(name = "idx_videos_processing_state", columnList = "processing_state"),
                @Index(name = "idx_videos_visibility", columnList = "visibility"),
                @Index(name = "idx_videos_publication_state", columnList = "publication_state"),
                @Index(name = "idx_videos_published_at", columnList = "published_at"),
                @Index(name = "idx_videos_category", columnList = "category_id"),
                @Index(name = "idx_videos_genre", columnList = "genre_id"),
                @Index(name = "idx_videos_language", columnList = "language_id"),
                @Index(name = "idx_videos_deleted", columnList = "deleted"),
                @Index(name = "idx_videos_creator_published", columnList = "creator_id, publication_state, published_at")
        })
@Getter
@Setter
public class Video extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "creator_id", nullable = false, updatable = false)
    private UUID creatorId;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private UUID channelId;


    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "slug", nullable = false, length = 220)
    private String slug;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "tags", length = 1000)
    private String tags; // comma-separated

    @Column(name = "genre_id")
    private UUID genreId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "language_id")
    private UUID languageId;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_state", nullable = false, length = 20)
    private ProcessingState processingState = ProcessingState.UPLOADING;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_state", nullable = false, length = 20)
    private PublicationState publicationState = PublicationState.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private long commentCount = 0;

    @Column(name = "share_count", nullable = false)
    private long shareCount = 0;

    @Column(name = "allow_comments", nullable = false)
    private boolean allowComments = true;

    @Column(name = "allow_embedding", nullable = false)
    private boolean allowEmbedding = true;

    @Column(name = "age_restricted", nullable = false)
    private boolean ageRestricted = false;

    @Column(name = "blocked", nullable = false)
    private boolean blocked = false;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_by")
    private UUID blockedBy;

    @Column(name = "source_object_key", length = 500)
    private String sourceObjectKey;

    @Column(name = "source_file_size")
    private Long sourceFileSize;

    @Column(name = "source_checksum", length = 64)
    private String sourceChecksum;

    @Column(name = "hls_manifest_key", length = 500)
    private String hlsManifestKey;

    @ElementCollection
    @CollectionTable(name = "video_resolutions", schema = "video_schema",
            joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "resolution")
    private Set<String> availableResolutions = new HashSet<>();

    @Column(name = "transcoding_job_id")
    private UUID transcodingJobId;

    @Column(name = "transcoding_error")
    private String transcodingError;

    @Column(name = "transcoding_progress")
    private Integer transcodingProgress = 0;

    @Column(name = "transcoding_started_at")
    private Instant transcodingStartedAt;

    @Column(name = "transcoding_completed_at")
    private Instant transcodingCompletedAt;

    public enum Visibility {
        PUBLIC, UNLISTED, PRIVATE
    }

    public enum ProcessingState {
        UPLOADING, UPLOADED, PROCESSING, READY, PROCESSING_FAILED
    }

    public enum PublicationState {
        DRAFT, PUBLISHED, UNPUBLISHED, SCHEDULED
    }

    public boolean isPublished() {
        return publicationState == PublicationState.PUBLISHED
                && processingState == ProcessingState.READY
                && !blocked
                && !isDeleted();
    }

    public boolean isProcessing() {
        return processingState == ProcessingState.PROCESSING
                || processingState == ProcessingState.UPLOADING
                || processingState == ProcessingState.UPLOADED;
    }

    public void markProcessingStarted(UUID jobId) {
        this.processingState = ProcessingState.PROCESSING;
        this.transcodingJobId = jobId;
        this.transcodingStartedAt = Instant.now();
        this.transcodingProgress = 0;
    }

    public void markProcessingCompleted(String hlsManifestKey, Set<String> resolutions,
                                         Integer durationSeconds, String thumbnailUrl) {
        this.processingState = ProcessingState.READY;
        this.hlsManifestKey = hlsManifestKey;
        this.availableResolutions = resolutions;
        this.durationSeconds = durationSeconds;
        this.thumbnailUrl = thumbnailUrl;
        this.transcodingCompletedAt = Instant.now();
        this.transcodingProgress = 100;
    }

    public void markProcessingFailed(String error) {
        this.processingState = ProcessingState.PROCESSING_FAILED;
        this.transcodingError = error;
        this.transcodingCompletedAt = Instant.now();
    }

    public void publish() {
        if (this.processingState != ProcessingState.READY) {
            throw new IllegalStateException("Cannot publish video that is not ready");
        }
        this.publicationState = PublicationState.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void unpublish() {
        this.publicationState = PublicationState.UNPUBLISHED;
    }

    public void block(UUID blockedBy, String reason) {
        this.blocked = true;
        this.blockedBy = blockedBy;
        this.blockedReason = reason;
        this.blockedAt = Instant.now();
    }

    public void unblock() {
        this.blocked = false;
        this.blockedBy = null;
        this.blockedReason = null;
        this.blockedAt = null;
    }
}
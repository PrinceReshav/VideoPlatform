package com.platform.creatorstudio.model;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing content associated with a studio.
 * In development, this uses local storage references.
 * In production, this would link to the main video service.
 */
@Entity
@Table(name = "studio_content", schema = "creator_studio_schema",
        indexes = {
                @Index(name = "idx_studio_content_studio", columnList = "studio_id"),
                @Index(name = "idx_studio_content_type", columnList = "content_type"),
                @Index(name = "idx_studio_content_published", columnList = "is_published"),
                @Index(name = "idx_studio_content_special", columnList = "is_special")
        })
@Getter
@Setter
public class StudioContent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The studio this content belongs to */
    @Column(name = "studio_id", nullable = false)
    private UUID studioId;

    /** Reference to content ID (in dev: local storage ID; in prod: video service ID) */
    @Column(name = "content_reference_id", nullable = false, length = 255)
    private String contentReferenceId;

    /** Type of content (VIDEO, IMAGE, DOCUMENT, AUDIO, etc.) */
    @Column(name = "content_type", length = 50, nullable = false)
    private String contentType;

    /** Title of the content */
    @Column(name = "title", length = 200)
    private String title;

    /** Description of the content */
    @Column(name = "description", length = 2000)
    private String description;

    /** Category of the content (matches studio categories) */
    @Column(name = "content_category", length = 50)
    private String contentCategory;

    /** Tags for the content (comma-separated) */
    @Column(name = "tags", length = 1000)
    private String tags;

    /** Whether the content is published/public */
    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    /** Whether the content is featured in Studios Special collection */
    @Column(name = "is_special", nullable = false)
    private boolean isSpecial = false;

    /** Position/order in the studio's content listing */
    @Column(name = "position")
    private Integer position = 0;

    /** Timestamp when the content was published */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** Timestamp when the content was added to the studio */
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;
// Content types supported
    public enum ContentType {
        VIDEO, IMAGE, DOCUMENT, AUDIO, LIVE_SESSION_REPLAY, POST, POLL
    }

    // Helper methods
    public boolean isPublishedAndNotSpecial() {
        return isPublished && !isSpecial;
    }

    public boolean isSpecialContent() {
        return isSpecial;
    }

    public void markAsSpecial() {
        this.isSpecial = true;
    }

    public void removeFromSpecial() {
        this.isSpecial = false;
    }

    public void publish() {
        this.isPublished = true;
        this.publishedAt = Instant.now();
    }

    public void unpublish() {
        this.isPublished = false;
        this.publishedAt = null;
    }
}
package com.platform.creatorstudio.dto;

import com.platform.creatorstudio.model.StudioContent;
import com.platform.creatorstudio.model.StudioContent.ContentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for StudioContent entity.
 * Used for transferring studio content data between processes.
 */
public class ContentDTO {

    private UUID id;
    private UUID studioId;
    private String contentReferenceId;
    private ContentType contentType;
    private String title;
    private String description;
    private String contentCategory;
    private String tags;
    private boolean isPublished;
    private boolean isSpecial;
    private Integer position;
    private Instant publishedAt;
    private Instant addedAt;
    private Instant updatedAt;

    // Constructors
    public ContentDTO() {}

    public ContentDTO(StudioContent content) {
        this.id = content.getId();
        this.studioId = content.getStudioId();
        this.contentReferenceId = content.getContentReferenceId();
        this.contentType = ContentType.valueOf(content.getContentType());
        this.title = content.getTitle();
        this.description = content.getDescription();
        this.contentCategory = content.getContentCategory();
        this.tags = content.getTags();
        this.isPublished = content.isPublished();
        this.isSpecial = content.isSpecial();
        this.position = content.getPosition();
        this.publishedAt = content.getPublishedAt();
        this.addedAt = content.getAddedAt();
        this.updatedAt = content.getUpdatedAt();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudioId() { return studioId; }
    public void setStudioId(UUID studioId) { this.studioId = studioId; }

    public String getContentReferenceId() { return contentReferenceId; }
    public void setContentReferenceId(String contentReferenceId) { this.contentReferenceId = contentReferenceId; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContentCategory() { return contentCategory; }
    public void setContentCategory(String contentCategory) { this.contentCategory = contentCategory; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { this.isPublished = published; }

    public boolean isSpecial() { return isSpecial; }
    public void setSpecial(boolean special) { this.isSpecial = special; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
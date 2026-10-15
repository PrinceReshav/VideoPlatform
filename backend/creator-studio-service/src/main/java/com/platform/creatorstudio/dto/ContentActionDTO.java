package com.platform.creatorstudio.dto;

import java.util.UUID;

/**
 * Data Transfer Object for studio content actions.
 * Used for receiving content action data from clients.
 */
public class ContentActionDTO {

    private UUID studioId;
    private String contentReferenceId;
    private String contentType; // Should match StudioContent.ContentType enum values
    private String title;
    private String description;
    private String contentCategory;
    private String tags;
    private boolean special;

    // Constructors
    public ContentActionDTO() {}

    // Getters and Setters
    public UUID getStudioId() { return studioId; }
    public void setStudioId(UUID studioId) { this.studioId = studioId; }

    public String getContentReferenceId() { return contentReferenceId; }
    public void setContentReferenceId(String contentReferenceId) { this.contentReferenceId = contentReferenceId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContentCategory() { return contentCategory; }
    public void setContentCategory(String contentCategory) { this.contentCategory = contentCategory; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public boolean isSpecial() { return special; }
    public void setSpecial(boolean special) { this.special = special; }
}
package com.platform.creatorstudio.dto;

import com.platform.creatorstudio.model.CreatorStudio;
import com.platform.creatorstudio.model.StudioCategory;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for CreatorStudio entity.
 * Used for transferring studio data between processes.
 */
public class StudioDTO {

    private UUID id;
    private String name;
    private String displayName;
    private String description;
    private String avatarUrl;
    private String bannerUrl;
    private StudioCategory primaryCategory;
    private String ownerType; // INDIVIDUAL or TEAM
    private UUID ownerId;
    private boolean isVerified;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public StudioDTO() {}

    public StudioDTO(CreatorStudio studio) {
        this.id = studio.getId();
        this.name = studio.getName();
        this.displayName = studio.getDisplayName();
        this.description = studio.getDescription();
        this.avatarUrl = studio.getAvatarUrl();
        this.bannerUrl = studio.getBannerUrl();
        this.primaryCategory = studio.getPrimaryCategory();
        this.ownerType = studio.getOwnerType().toString();
        this.ownerId = studio.getOwnerId();
        this.isVerified = studio.isVerified();
        this.isActive = studio.isActive();
        this.createdAt = studio.getCreatedAt();
        this.updatedAt = studio.getUpdatedAt();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public StudioCategory getPrimaryCategory() { return primaryCategory; }
    public void setPrimaryCategory(StudioCategory primaryCategory) { this.primaryCategory = primaryCategory; }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
package com.platform.creatorstudio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a CreatorStudio.
 * Used for receiving studio creation data from clients.
 */
public class StudioCreateDTO {

    @NotBlank(message = "Studio name is required")
    @Size(min = 3, max = 100, message = "Studio name must be between 3 and 100 characters")
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._ -]*", message = "Studio name may contain letters, numbers, spaces, dots, underscores and hyphens")
    private String name;
    @Size(max = 150, message = "Display name must not exceed 150 characters")
    private String displayName;
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
    @Size(max = 500, message = "Banner URL must not exceed 500 characters")
    private String bannerUrl;
    @NotBlank(message = "Primary category is required")
    private String primaryCategory; // Should match StudioCategory enum values
    private String ownerType; // Should match CreatorStudio.OwnerType enum values

    // Constructors
    public StudioCreateDTO() {}

    // Getters and Setters
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

    public String getPrimaryCategory() { return primaryCategory; }
    public void setPrimaryCategory(String primaryCategory) { this.primaryCategory = primaryCategory; }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
}
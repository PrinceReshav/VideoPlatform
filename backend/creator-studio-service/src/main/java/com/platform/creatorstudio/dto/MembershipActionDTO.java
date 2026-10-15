package com.platform.creatorstudio.dto;

import java.util.UUID;

/**
 * Data Transfer Object for studio membership actions.
 * Used for receiving membership action data from clients.
 */
public class MembershipActionDTO {

    private UUID studioId;
    private UUID userId;
    private String role; // Should match StudioMembership.StudioRole enum values

    // Constructors
    public MembershipActionDTO() {}

    // Getters and Setters
    public UUID getStudioId() { return studioId; }
    public void setStudioId(UUID studioId) { this.studioId = studioId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
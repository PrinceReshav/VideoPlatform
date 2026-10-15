package com.platform.creatorstudio.dto;

import java.util.UUID;

/**
 * Data Transfer Object for studio invitation actions.
 * Used for receiving invitation action data from clients.
 */
public class InvitationActionDTO {

    private UUID studioId;
    private String email;
    private String role; // Should match StudioInvitation.StudioRole enum values
    private int expiresInHours; // Hours until invitation expires (default: 72)

    // Constructors
    public InvitationActionDTO() {}

    // Getters and Setters
    public UUID getStudioId() { return studioId; }
    public void setStudioId(UUID studioId) { this.studioId = studioId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getExpiresInHours() { return expiresInHours; }
    public void setExpiresInHours(int expiresInHours) { this.expiresInHours = expiresInHours; }
}
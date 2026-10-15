package com.platform.creatorstudio.dto;

import com.platform.creatorstudio.model.StudioInvitation;
import com.platform.creatorstudio.model.StudioInvitation.StudioRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for StudioInvitation entity.
 * Used for transferring studio invitation data between processes.
 */
public class InvitationDTO {

    private UUID id;
    private UUID studioId;
    private String email;
    private UUID userId;
    private StudioRole role;
    private String invitationToken;
    private Instant sentAt;
    private Instant expiresAt;
    private Instant acceptedAt;
    private boolean isAccepted;

    // Constructors
    public InvitationDTO() {}

    public InvitationDTO(StudioInvitation invitation) {
        this.id = invitation.getId();
        this.studioId = invitation.getStudioId();
        this.email = invitation.getEmail();
        this.userId = invitation.getUserId();
        this.role = invitation.getRole();
        this.invitationToken = invitation.getInvitationToken();
        this.sentAt = invitation.getSentAt();
        this.expiresAt = invitation.getExpiresAt();
        this.acceptedAt = invitation.getAcceptedAt();
        this.isAccepted = invitation.isAccepted();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudioId() { return studioId; }
    public void setStudioId(UUID studioId) { this.studioId = studioId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public StudioRole getRole() { return role; }
    public void setRole(StudioRole role) { this.role = role; }

    public String getInvitationToken() { return invitationToken; }
    public void setInvitationToken(String invitationToken) { this.invitationToken = invitationToken; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }

    public boolean isAccepted() { return isAccepted; }
    public void setAccepted(boolean accepted) { this.isAccepted = accepted; }
}
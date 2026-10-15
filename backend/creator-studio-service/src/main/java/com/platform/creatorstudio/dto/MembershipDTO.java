package com.platform.creatorstudio.dto;

import com.platform.creatorstudio.model.StudioMembership;
import com.platform.creatorstudio.model.StudioMembership.StudioRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for StudioMembership entity.
 * Used for transferring studio membership data between processes.
 */
public class MembershipDTO {

    private UUID id;
    private UUID studioId;
    private UUID userId;
    private StudioRole role;
    private boolean isActive;
    private Instant joinedAt;
    private Instant updatedAt;

    // Constructors
    public MembershipDTO() {}

    public MembershipDTO(StudioMembership membership) {
        this.id = membership.getId();
        this.studioId = membership.getStudioId();
        this.userId = membership.getUserId();
        this.role = membership.getRole();
        this.isActive = membership.isActive();
        this.joinedAt = membership.getJoinedAt();
        this.updatedAt = membership.getUpdatedAt();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudioId() { return studioId; }
    public void setStudioId(UUID studioId) { this.studioId = studioId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public StudioRole getRole() { return role; }
    public void setRole(StudioRole role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
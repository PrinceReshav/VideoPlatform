package com.platform.creatorstudio.model;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user's membership in a studio.
 * Defines the role and permissions of a user within a studio.
 */
@Entity
@Table(name = "studio_memberships", schema = "creator_studio_schema",
        indexes = {
                @Index(name = "idx_studio_memberships_studio", columnList = "studio_id"),
                @Index(name = "idx_studio_memberships_user", columnList = "user_id"),
                @Index(name = "idx_studio_memberships_role", columnList = "role")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_studio_memberships_studio_user", columnNames = {"studio_id", "user_id"})
        })
@Getter
@Setter
public class StudioMembership extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The studio this membership belongs to */
    @Column(name = "studio_id", nullable = false)
    private UUID studioId;

    /** The user who is a member of the studio */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The role of the user in the studio */
    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private StudioRole role;

    /** Whether the membership is active (can be deactivated without deletion) */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Timestamp when the membership was created */
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;
public enum StudioRole {
        OWNER,      // Studio owner (highest privileges)
        ADMIN,      // Studio administrator (can manage settings, members, content)
        MANAGER,    // Studio manager (can manage content, moderate sessions)
        MODERATOR,  // Content moderator (can moderate content and comments)
        CREATOR,    // Content creator (can upload and manage own content)
        MEMBER      // Regular member (can view content, participate in discussions)
    }

    // Helper methods
    public boolean isOwner() {
        return role == StudioRole.OWNER;
    }

    public boolean isAdminOrOwner() {
        return role == StudioRole.ADMIN || role == StudioRole.OWNER;
    }

    public boolean canManageMembers() {
        return role == StudioRole.OWNER || role == StudioRole.ADMIN;
    }

    public boolean canManageContent() {
        return role == StudioRole.OWNER || role == StudioRole.ADMIN ||
               role == StudioRole.MANAGER || role == StudioRole.CREATOR;
    }

    public boolean canModerate() {
        return role == StudioRole.OWNER || role == StudioRole.ADMIN ||
               role == StudioRole.MANAGER || role == StudioRole.MODERATOR;
    }

    public boolean isActiveMember() {
        return isActive;
    }
}
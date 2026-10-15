package com.platform.creatorstudio.model;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an invitation to join a studio.
 * Tracks pending invitations sent to users.
 */
@Entity
@Table(name = "studio_invitations", schema = "creator_studio_schema",
        indexes = {
                @Index(name = "idx_studio_invitations_studio", columnList = "studio_id"),
                @Index(name = "idx_studio_invitations_email", columnList = "email"),
                @Index(name = "idx_studio_invitations_token", columnList = "invitation_token")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_studio_invitations_token", columnNames = "invitation_token")
        })
@Getter
@Setter
public class StudioInvitation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The studio sending the invitation */
    @Column(name = "studio_id", nullable = false)
    private UUID studioId;

    /** Email address of the invited user */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** Optional user ID if the user already exists in the system */
    @Column(name = "user_id")
    private UUID userId;

    /** Role to assign to the user upon accepting the invitation */
    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private StudioRole role;

    /** Unique token for the invitation (used in invitation link) */
    @Column(name = "invitation_token", nullable = false, length = 100)
    private String invitationToken;

    /** Timestamp when the invitation expires */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Timestamp when the invitation was accepted */
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    /** Whether the invitation has been accepted */
    @Column(name = "is_accepted", nullable = false)
    private boolean isAccepted = false;

    /** Timestamp when the invitation was sent */
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    // Reference to StudioRole enum from StudioMembership
    public enum StudioRole {
        OWNER, ADMIN, MANAGER, MODERATOR, CREATOR, MEMBER
    }

    // Helper methods
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return !isAccepted && !isExpired();
    }

    public boolean canBeAcceptedBy(UUID userId) {
        return userId != null && userId.equals(this.userId);
    }

    public boolean canBeAcceptedByEmail(String email) {
        return email != null && email.equalsIgnoreCase(this.email);
    }
}
package com.platform.creatorstudio.service;

import com.platform.creatorstudio.model.StudioInvitation;
import com.platform.creatorstudio.model.StudioMembership;
import com.platform.creatorstudio.model.StudioInvitation.StudioRole;
import com.platform.creatorstudio.repository.StudioInvitationRepository;
import com.platform.creatorstudio.repository.CreatorStudioRepository;
import com.platform.creatorstudio.repository.StudioMembershipRepository;
import com.platform.common.security.CurrentUserAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for managing studio invitations.
 * Handles sending, accepting, and expiring studio invitations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudioInvitationService {

    private final StudioInvitationRepository invitationRepository;
    private final CreatorStudioRepository studioRepository;
    private final StudioMembershipRepository membershipRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Sends an invitation to join a studio.
     *
     * @param studioId The studio ID
     * @param email The email address to invite
     * @param role The role to assign upon acceptance
     * @param expiresInHours How many hours until the invitation expires (default: 72)
     * @return The created invitation
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#studioId)")
    public StudioInvitation sendInvitation(UUID studioId, String email, StudioRole role, int expiresInHours) {
        // Verify studio exists
        var studio = studioRepository.findById(studioId);
        if (studio.isEmpty() || !studio.get().isActiveAndNotDeleted()) {
            throw new IllegalArgumentException("Studio not found or inactive: " + studioId);
        }

        // Validate email
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        String normalizedEmail = email.trim().toLowerCase();

        // Check if user with this email is already a member
        // Note: In a real system, we'd look up the user by email first
        // For now, we'll just prevent duplicate invitations to the same email
        if (invitationRepository.findByEmailAndIsAcceptedFalse(normalizedEmail).stream()
                .anyMatch(inv -> inv.getStudioId().equals(studioId))) {
            throw new IllegalArgumentException("Invitation already sent to this email for this studio");
        }

        // Generate a unique invitation token
        String token = generateInvitationToken();

        StudioInvitation invitation = new StudioInvitation();
        invitation.setStudioId(studioId);
        invitation.setEmail(normalizedEmail);
        invitation.setRole(role);
        invitation.setInvitationToken(token);
        invitation.setSentAt(Instant.now());
        invitation.setExpiresAt(Instant.now().plusSeconds(expiresInHours * 3600L));
        invitation.setUpdatedAt(Instant.now());

        StudioInvitation savedInvitation = invitationRepository.save(invitation);
        log.info("Invitation sent to {} for studio {} by user {}", email, studioId, getCurrentUserId());
        return savedInvitation;
    }


    public StudioMembership acceptInvitation(String token) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        String currentEmail = currentUserAccessor.getCurrentEmail();
        if (userId == null || currentEmail == null || currentEmail.isBlank()) {
            throw new SecurityException("An authenticated user with a verified email is required");
        }

        // Find the invitation by token
        var invitation = invitationRepository.findByInvitationToken(token);
        if (invitation.isEmpty()) {
            throw new IllegalArgumentException("Invalid invitation token");
        }

        StudioInvitation inv = invitation.get();

        // Check if invitation has expired
        if (inv.isExpired()) {
            throw new IllegalArgumentException("Invitation has expired");
        }

        // Check if invitation has already been accepted
        if (inv.isAccepted()) {
            throw new IllegalArgumentException("Invitation has already been accepted");
        }

        // The invitation belongs to the current authenticated identity only.
        boolean matchesByEmail = inv.getEmail() != null
                && inv.getEmail().trim().equalsIgnoreCase(currentEmail.trim());
        boolean matchesByUserId = inv.getUserId() == null || inv.getUserId().equals(userId);
        if (!matchesByEmail || !matchesByUserId) {
            throw new SecurityException("Invitation is not addressed to the authenticated user");
        }

        // Verify studio still exists and is active
        var studio = studioRepository.findById(inv.getStudioId());
        if (studio.isEmpty() || !studio.get().isActiveAndNotDeleted()) {
            throw new IllegalArgumentException("Studio no longer available");
        }

        // Check if user is already a member of the studio
        if (membershipRepository.existsByStudioIdAndUserIdAndIsActiveTrue(inv.getStudioId(), userId)) {
            throw new IllegalArgumentException("User is already a member of this studio");
        }

        // Create the membership
        StudioMembership membership = new StudioMembership();
        membership.setStudioId(inv.getStudioId());
        membership.setUserId(userId);
        membership.setRole(StudioMembership.StudioRole.valueOf(inv.getRole().name()));
        membership.setJoinedAt(Instant.now());
        membership.setUpdatedAt(Instant.now());
        membership.setActive(true);

        StudioMembership savedMembership = membershipRepository.save(membership);

        // Mark invitation as accepted
        inv.setAccepted(true);
        inv.setAcceptedAt(Instant.now());
        inv.setUpdatedAt(Instant.now());
        invitationRepository.save(inv);

        log.info("Invitation {} accepted by user {} for studio {}", token, userId, inv.getStudioId());
        return savedMembership;
    }

    /**
     * Revokes an invitation.
     *
     * @param token The invitation token
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(@studioInvitationService.getStudioIdByToken(#token))")
    public void revokeInvitation(String token) {
        var invitation = invitationRepository.findByInvitationToken(token);
        if (invitation.isPresent()) {
            StudioInvitation inv = invitation.get();
            // Only allow revocation by studio admins/owners or system admins
            UUID userId = currentUserAccessor.getCurrentUserId();
            if (userId != null) {
                boolean isSystemAdmin = currentUserAccessor.hasRole("ADMIN");
                boolean isStudioAdmin = false;

                if (!isSystemAdmin) {
                    StudioMembership membership = membershipRepository.findByStudioIdAndUserId(
                            inv.getStudioId(), userId).orElse(null);
                    isStudioAdmin = membership != null &&
                            (membership.getRole() == StudioMembership.StudioRole.OWNER ||
                                    membership.getRole() == StudioMembership.StudioRole.ADMIN);
                }

                if (!isSystemAdmin && !isStudioAdmin) {
                    throw new SecurityException("Not authorized to revoke this invitation");
                }
            }

            invitationRepository.delete(inv);
            log.info("Invitation {} revoked", token);
        }
    }

    /**
     * Gets an invitation by its token.
     *
     * @param token The invitation token
     * @return The invitation if found, null otherwise
     */
    public StudioInvitation getInvitationByToken(String token) {
        return invitationRepository.findByInvitationToken(token).orElse(null);
    }

    /**
     * Gets pending invitations for a studio.
     *
     * @param studioId The studio ID
     * @return List of pending invitations
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#studioId)")
    public List<StudioInvitation> getPendingInvitations(UUID studioId) {
        return invitationRepository.findByStudioIdAndIsAcceptedFalse(studioId);
    }

    /**
     * Gets pending invitations for an email address.
     *
     * @param email The email address
     * @return List of pending invitations
     */
    public List<StudioInvitation> getPendingInvitationsByEmail(String email) {
        if (email == null) {
            return List.of();
        }
        return invitationRepository.findByEmailAndIsAcceptedFalse(email.trim().toLowerCase());
    }

    /**
     * Expires old invitations (cleanup).
     *
     * @return Number of invitations expired
     */
    public int expireOldInvitations() {
        Instant now = Instant.now();
        List<StudioInvitation> expired = invitationRepository.findExpiredInvitations(now);
        int count = expired.size();

        for (StudioInvitation invitation : expired) {
            invitationRepository.delete(invitation);
        }

        if (count > 0) {
            log.info("Expired {} invitations", count);
        }
        return count;
    }

    /**
     * Generates a unique invitation token.
     *
     * @return A unique token string
     */
    private String generateInvitationToken() {
        // Generate a random URL-safe token
        StringBuilder token = new StringBuilder(32);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        for (int i = 0; i < 32; i++) {
            int index = ThreadLocalRandom.current().nextInt(chars.length());
            token.append(chars.charAt(index));
        }
        return token.toString();
    }

    /**
     * Gets the current user's ID from the authentication context.
     *
     * @return The current user's ID
     */
    protected UUID getCurrentUserId() {
        return currentUserAccessor.getCurrentUserId();
    }

    /** Resolves the parent studio for authorization of invitation-token operations. */
    public UUID getStudioIdByToken(String token) {
        if (token == null || token.isBlank()) return null;
        return invitationRepository.findByInvitationToken(token)
                .map(StudioInvitation::getStudioId)
                .orElse(null);
    }
}
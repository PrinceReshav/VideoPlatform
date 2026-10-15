package com.platform.creatorstudio.service;

import com.platform.creatorstudio.model.StudioMembership;
import com.platform.creatorstudio.model.StudioMembership.StudioRole;
import com.platform.creatorstudio.repository.StudioMembershipRepository;
import com.platform.creatorstudio.repository.CreatorStudioRepository;
import com.platform.common.security.CurrentUserAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing studio memberships.
 * Handles user membership in studios, role assignments, and permissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudioMembershipService {

    private final StudioMembershipRepository membershipRepository;
    private final CreatorStudioRepository studioRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Adds a user as a member of a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID to add as member
     * @param role The role to assign to the user
     * @return The created membership
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#studioId)")
    public StudioMembership addMember(UUID studioId, UUID userId, StudioRole role) {
        // Verify studio exists
        var studio = studioRepository.findById(studioId);
        if (studio.isEmpty() || !studio.get().isActiveAndNotDeleted()) {
            throw new IllegalArgumentException("Studio not found or inactive: " + studioId);
        }

        // Check if user is already a member
        if (membershipRepository.existsByStudioIdAndUserIdAndIsActiveTrue(studioId, userId)) {
            throw new IllegalArgumentException("User is already a member of this studio");
        }

        StudioMembership membership = new StudioMembership();
        membership.setStudioId(studioId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setJoinedAt(Instant.now());
        membership.setUpdatedAt(Instant.now());
        membership.setActive(true);

        StudioMembership savedMembership = membershipRepository.save(membership);
        log.info("User {} added as {} to studio {}", userId, role, studioId);
        return savedMembership;
    }

    /**
     * Removes a user from a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID to remove
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#studioId)")
    public void removeMember(UUID studioId, UUID userId) {
        // Verify studio exists
        var studio = studioRepository.findById(studioId);
        if (studio.isEmpty() || !studio.get().isActiveAndNotDeleted()) {
            throw new IllegalArgumentException("Studio not found or inactive: " + studioId);
        }

        // Find the membership
        var membership = membershipRepository.findByStudioIdAndUserId(studioId, userId);
        if (membership.isEmpty()) {
            throw new IllegalArgumentException("User is not a member of this studio");
        }

        // Check if trying to remove the last owner
        if (membership.get().getRole() == StudioRole.OWNER) {
            long ownerCount = membershipRepository.countByStudioIdAndRoleAndIsActiveTrue(
                    studioId, StudioRole.OWNER);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last owner of a studio");
            }
        }

        // Soft delete the membership
        membership.get().setActive(false);
        membership.get().setUpdatedAt(Instant.now());
        membershipRepository.save(membership.get());

        log.info("User {} removed from studio {}", userId, studioId);
    }

    /**
     * Updates a user's role in a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID
     * @param newRole The new role to assign
     * @return The updated membership
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#studioId)")
    public StudioMembership updateMemberRole(UUID studioId, UUID userId, StudioRole newRole) {
        // Verify studio exists
        var studio = studioRepository.findById(studioId);
        if (studio.isEmpty() || !studio.get().isActiveAndNotDeleted()) {
            throw new IllegalArgumentException("Studio not found or inactive: " + studioId);
        }

        // Find the membership
        var membership = membershipRepository.findByStudioIdAndUserId(studioId, userId);
        if (membership.isEmpty()) {
            throw new IllegalArgumentException("User is not a member of this studio");
        }

        // Check if trying to remove the last owner by changing role
        if (membership.get().getRole() == StudioRole.OWNER && newRole != StudioRole.OWNER) {
            long ownerCount = membershipRepository.countByStudioIdAndRoleAndIsActiveTrue(
                    studioId, StudioRole.OWNER);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot change the last owner's role away from OWNER");
            }
        }

        // Update the role
        membership.get().setRole(newRole);
        membership.get().setUpdatedAt(Instant.now());

        StudioMembership updatedMembership = membershipRepository.save(membership.get());
        log.info("User {} role updated to {} in studio {}", userId, newRole, studioId);
        return updatedMembership;
    }

    /**
     * Gets a user's membership in a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID
     * @return The membership if found, null otherwise
     */
    public StudioMembership getMembership(UUID studioId, UUID userId) {
        return membershipRepository.findByStudioIdAndUserId(studioId, userId).orElse(null);
    }

    /**
     * Gets all members of a studio.
     *
     * @param studioId The studio ID
     * @return List of all active members in the studio
     */
    public List<StudioMembership> getStudioMembers(UUID studioId) {
        return membershipRepository.findByStudioIdAndIsActiveTrue(studioId);
    }

    /**
     * Gets all studios a user is a member of.
     *
     * @param userId The user ID
     * @return List of all studio memberships for the user
     */
    public List<StudioMembership> getUserMemberships(UUID userId) {
        return membershipRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Checks if a user is a member of a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID
     * @return true if the user is a member, false otherwise
     */
    public boolean isMember(UUID studioId, UUID userId) {
        return membershipRepository.existsByStudioIdAndUserIdAndIsActiveTrue(studioId, userId);
    }

    /**
     * Gets a user's role in a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID
     * @return The user's role if they are a member, null otherwise
     */
    public StudioRole getUserRole(UUID studioId, UUID userId) {
        return membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(studioId, userId)
                .orElse(null);
    }

    /**
     * Checks if the current user can manage members of a studio.
     * This is used in SpEL expressions for method security.
     *
     * @param studioId The studio ID
     * @return true if the current user can manage members, false otherwise
     */
    public boolean canManageMembers(UUID studioId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // Check if user is admin
        if (currentUserAccessor.hasRole("ADMIN")) {
            return true;
        }

        // Support legacy studios whose owner membership row predates automatic owner membership creation.
        if (isStudioOwner(studioId, userId)) {
            return true;
        }
        StudioRole userRole = getUserRole(studioId, userId);
        return userRole != null &&
               (userRole == StudioRole.OWNER || userRole == StudioRole.ADMIN);
    }

    /**
     * Checks if the current user can manage content of a studio.
     * This is used in SpEL expressions for method security.
     *
     * @param studioId The studio ID
     * @return true if the current user can manage content, false otherwise
     */
    public boolean canManageContent(UUID studioId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // Check if user is admin
        if (currentUserAccessor.hasRole("ADMIN")) {
            return true;
        }

        if (isStudioOwner(studioId, userId)) {
            return true;
        }
        // Check if user has a role that can manage content
        StudioRole userRole = getUserRole(studioId, userId);
        return userRole != null &&
               (userRole == StudioRole.OWNER ||
                userRole == StudioRole.ADMIN ||
                userRole == StudioRole.MANAGER ||
                userRole == StudioRole.CREATOR);
    }

    /**
     * Checks if the current user can view content of a studio.
     * This is used in SpEL expressions for method security.
     *
     * @param studioId The studio ID
     * @return true if the current user can view content, false otherwise
     */
    public boolean canViewContent(UUID studioId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // Check if user is admin
        if (currentUserAccessor.hasRole("ADMIN")) {
            return true;
        }

        if (isStudioOwner(studioId, userId)) {
            return true;
        }
        // Check if user is a member of the studio (any role)
        StudioRole userRole = getUserRole(studioId, userId);
        return userRole != null;
    }

    /**
     * Checks if the current user can view members of a studio.
     * This is used in SpEL expressions for method security.
     *
     * @param studioId The studio ID
     * @return true if the current user can view members, false otherwise
     */
    public boolean canViewMembers(UUID studioId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // Check if user is admin
        if (currentUserAccessor.hasRole("ADMIN")) {
            return true;
        }

        if (isStudioOwner(studioId, userId)) {
            return true;
        }
        // Check if user is a member of the studio (any role)
        StudioRole userRole = getUserRole(studioId, userId);
        return userRole != null;
    }

    /**
     * Gets the current user's ID from the authentication context.
     *
     * @return The current user's ID
     */
    protected UUID getCurrentUserId() {
        return currentUserAccessor.getCurrentUserId();
    }

    private boolean isStudioOwner(UUID studioId, UUID userId) {
        if (studioId == null || userId == null) return false;
        return studioRepository.findById(studioId)
                .map(studio -> userId.equals(studio.getOwnerId()) && studio.isActiveAndNotDeleted())
                .orElse(false);
    }
}
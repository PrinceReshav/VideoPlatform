package com.platform.creatorstudio.service;

import com.platform.creatorstudio.model.CreatorStudio;
import com.platform.creatorstudio.model.CreatorStudio.OwnerType;
import com.platform.creatorstudio.model.StudioCategory;
import com.platform.creatorstudio.repository.CreatorStudioRepository;
import com.platform.creatorstudio.repository.StudioMembershipRepository;
import com.platform.creatorstudio.model.StudioMembership;
import com.platform.creatorstudio.model.StudioMembership.StudioRole;
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
 * Service for managing creator studios.
 * Handles studio creation, retrieval, updates, and deletion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreatorStudioService {

    private final CreatorStudioRepository studioRepository;
    private final StudioMembershipRepository membershipRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final StudioNameAvailabilityService nameAvailabilityService;

    /**
     * Creates a new studio.
     *
     * @param name The unique studio name
     * @param displayName The display name for the studio
     * @param description The studio description
     * @param avatarUrl URL to studio avatar
     * @param bannerUrl URL to studio banner
     * @param primaryCategory The primary category of the studio
     * @param ownerType Whether the studio is owned by an individual or team
     * @return The created studio
     */
    @PreAuthorize("isAuthenticated()")
    public CreatorStudio createStudio(String name, String displayName, String description,
                                      String avatarUrl, String bannerUrl,
                                      StudioCategory primaryCategory, OwnerType ownerType) {
        // Normalize the studio name (trim and prepare for case-insensitive comparison)
        String normalizedName = name != null ? name.trim() : "";

        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Studio name cannot be empty");
        }

        // Check if studio name already exists using Bloom Filter optimization
        if (!nameAvailabilityService.isStudioNameAvailable(normalizedName)) {
            throw new IllegalArgumentException("Studio name already exists: " + normalizedName);
        }

        UUID userId = currentUserAccessor.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Current user not found");
        }

        CreatorStudio studio = new CreatorStudio();
        studio.setName(normalizedName);
        studio.setDisplayName(displayName != null ? displayName : normalizedName);
        studio.setDescription(description);
        studio.setAvatarUrl(avatarUrl);
        studio.setBannerUrl(bannerUrl);
        studio.setPrimaryCategory(primaryCategory);
        studio.setOwnerType(ownerType);
        studio.setOwnerId(userId);
        studio.setCreatedAt(Instant.now());
        studio.setUpdatedAt(Instant.now());

        CreatorStudio savedStudio = studioRepository.save(studio);
        // Every studio starts with an OWNER membership so membership-based permissions work immediately.
        StudioMembership ownerMembership = new StudioMembership();
        ownerMembership.setStudioId(savedStudio.getId());
        ownerMembership.setUserId(userId);
        ownerMembership.setRole(StudioRole.OWNER);
        ownerMembership.setActive(true);
        ownerMembership.setJoinedAt(Instant.now());
        ownerMembership.setUpdatedAt(Instant.now());
        membershipRepository.save(ownerMembership);

        // Add the new studio name to the Bloom Filter for future availability checks.
        nameAvailabilityService.addStudioName(savedStudio.getName());
        log.info("Studio created: {} by user {}", savedStudio.getId(), userId);
        return savedStudio;
    }

    /**
     * Gets a studio by its ID.
     *
     * @param studioId The studio ID
     * @return The studio if found, null otherwise
     */
    public CreatorStudio getStudioById(UUID studioId) {
        return studioRepository.findById(studioId).orElse(null);
    }

    /**
     * Gets a studio by its name (case-insensitive).
     *
     * @param name The studio name
     * @return The studio if found, null otherwise
     */
    public CreatorStudio getStudioByName(String name) {
        return studioRepository.findByNameIgnoreCase(
                name != null ? name.trim() : ""
        ).orElse(null);
    }
    /**
     * Gets all studios owned by the current user.
     *
     * @return List of studios owned by the current user
     */
    @PreAuthorize("isAuthenticated()")
    public List<CreatorStudio> getStudiosByCurrentUser() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        return studioRepository.findByOwnerIdAndIsActiveTrueAndDeletedFalse(userId);
    }

    /**
     * Gets all active studios.
     *
     * @return List of all active studios
     */
    public List<CreatorStudio> getAllActiveStudios() {
        return studioRepository.findByIsActiveTrueAndDeletedFalse();
    }

    /**
     * Gets studios by primary category.
     *
     * @param category The studio category
     * @return List of studios in the specified category
     */
    public List<CreatorStudio> getStudiosByCategory(StudioCategory category) {
        return studioRepository.findByPrimaryCategory(category);
    }

    /**
     * Updates a studio's information.
     *
     * @param studioId The studio ID
     * @param displayName The new display name
     * @param description The new description
     * @param avatarUrl The new avatar URL
     * @param bannerUrl The new banner URL
     * @param primaryCategory The new primary category
     * @return The updated studio
     */
    @PreAuthorize("isAuthenticated()")
    public CreatorStudio updateStudio(UUID studioId, String displayName, String description,
                                      String avatarUrl, String bannerUrl,
                                      StudioCategory primaryCategory) {
        CreatorStudio studio = getStudioById(studioId);
        if (studio == null) {
            throw new IllegalArgumentException("Studio not found: " + studioId);
        }

        // Check if current user is the owner or an admin
        UUID userId = currentUserAccessor.getCurrentUserId();
        boolean isOwner = studio.getOwnerId() != null && studio.getOwnerId().equals(userId);
        boolean isAdmin = currentUserAccessor.hasRole("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new SecurityException("User is not authorized to update this studio");
        }

        studio.setDisplayName(displayName != null ? displayName : studio.getDisplayName());
        studio.setDescription(description != null ? description : studio.getDescription());
        studio.setAvatarUrl(avatarUrl != null ? avatarUrl : studio.getAvatarUrl());
        studio.setBannerUrl(bannerUrl != null ? bannerUrl : studio.getBannerUrl());
        studio.setPrimaryCategory(primaryCategory != null ? primaryCategory : studio.getPrimaryCategory());
        studio.setUpdatedAt(Instant.now());

        CreatorStudio updatedStudio = studioRepository.save(studio);
        log.info("Studio updated: {} by user {}", studioId, userId);
        return updatedStudio;
    }

    /**
     * Deactivates a studio (soft delete).
     *
     * @param studioId The studio ID
     */
    @PreAuthorize("isAuthenticated()")
    public void deactivateStudio(UUID studioId) {
        CreatorStudio studio = getStudioById(studioId);
        if (studio == null) {
            throw new IllegalArgumentException("Studio not found: " + studioId);
        }

        // Check if current user is the owner or an admin
        UUID userId = currentUserAccessor.getCurrentUserId();
        boolean isOwner = studio.getOwnerId() != null && studio.getOwnerId().equals(userId);
        boolean isAdmin = currentUserAccessor.hasRole("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new SecurityException("User is not authorized to deactivate this studio");
        }

        studio.setActive(false);
        studio.setDeleted(true);
        studio.setUpdatedAt(Instant.now());

        studioRepository.save(studio);
        log.info("Studio deactivated: {} by user {}", studioId, userId);
    }

    /**
     * Activates a previously deactivated studio.
     *
     * @param studioId The studio ID
     */
    @PreAuthorize("isAuthenticated()")
    public void activateStudio(UUID studioId) {
        CreatorStudio studio = getStudioById(studioId);
        if (studio == null) {
            throw new IllegalArgumentException("Studio not found: " + studioId);
        }

        // Check if current user is the owner or an admin
        UUID userId = currentUserAccessor.getCurrentUserId();
        boolean isOwner = studio.getOwnerId() != null && studio.getOwnerId().equals(userId);
        boolean isAdmin = currentUserAccessor.hasRole("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new SecurityException("User is not authorized to activate this studio");
        }

        studio.setActive(true);
        studio.setDeleted(false);
        studio.setUpdatedAt(Instant.now());

        studioRepository.save(studio);
        log.info("Studio activated: {} by user {}", studioId, userId);
    }

    /**
     * Checks if a studio name is available.
     *
     * @param name The studio name to check
     * @return true if the name is available, false otherwise
     */
    public boolean isStudioNameAvailable(String name) {
        return nameAvailabilityService.isStudioNameAvailable(name);
    }

    /**
     * Gets the current user's ID from the authentication context.
     *
     * @return The current user's ID
     */
    protected UUID getCurrentUserId() {
        return currentUserAccessor.getCurrentUserId();
    }
}
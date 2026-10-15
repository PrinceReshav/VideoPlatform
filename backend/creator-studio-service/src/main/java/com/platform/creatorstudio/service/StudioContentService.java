package com.platform.creatorstudio.service;

import com.platform.creatorstudio.model.StudioContent;
import com.platform.creatorstudio.model.StudioContent.ContentType;
import com.platform.creatorstudio.repository.StudioContentRepository;
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
 * Service for managing studio content.
 * Handles content association with studios, including uploading,
 * organizing, and featuring content in Studios Special collections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudioContentService {

    private final StudioContentRepository contentRepository;
    private final CreatorStudioRepository studioRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Adds content to a studio.
     *
     * @param studioId The studio ID
     * @param contentReferenceId Reference to the content (in dev: local storage ID)
     * @param contentType The type of content (VIDEO, IMAGE, etc.)
     * @param title The title of the content
     * @param description The description of the content
     * @param contentCategory The category of the content
     * @param tags Tags for the content (comma-separated)
     * @param isSpecial Whether to feature this content in Studios Special
     * @return The created studio content association
     */
    @PreAuthorize("@studioMembershipService.canManageContent(#studioId)")
    public StudioContent addContent(UUID studioId, String contentReferenceId, ContentType contentType,
                                    String title, String description, String contentCategory,
                                    String tags, boolean isSpecial) {
        // Verify studio exists
        var studio = studioRepository.findById(studioId);
        if (studio.isEmpty() || !studio.get().isActiveAndNotDeleted()) {
            throw new IllegalArgumentException("Studio not found or inactive: " + studioId);
        }

        // Validate required fields
        if (contentReferenceId == null || contentReferenceId.isEmpty()) {
            throw new IllegalArgumentException("Content reference ID cannot be empty");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Content title cannot be empty");
        }

        // Check if content already exists in this studio
        if (contentRepository.findByContentReferenceId(contentReferenceId).isPresent()) {
            throw new IllegalArgumentException("Content already associated with a studio");
        }

        StudioContent content = new StudioContent();
        content.setStudioId(studioId);
        content.setContentReferenceId(contentReferenceId);
        content.setContentType((contentType != null ? contentType : ContentType.VIDEO).name());
        content.setTitle(title.trim());
        content.setDescription(description != null ? description : "");
        content.setContentCategory(contentCategory != null ? contentCategory : "");
        content.setTags(tags != null ? tags : "");
        content.setSpecial(isSpecial);
        content.setPublished(true); // Content is published when added to studio
        content.setPublishedAt(Instant.now());
        content.setAddedAt(Instant.now());
        content.setUpdatedAt(Instant.now());

        // Set position to be at the end of the current list
        int maxPosition = contentRepository.findByStudioIdOrderByPositionAsc(studioId)
                .stream()
                .mapToInt(StudioContent::getPosition)
                .max()
                .orElse(-1);
        content.setPosition(maxPosition + 1);

        StudioContent savedContent = contentRepository.save(content);
        log.info("Content added to studio {}: {}", studioId, savedContent.getId());
        return savedContent;
    }

    /**
     * Gets content by its ID.
     *
     * @param contentId The content ID
     * @return The content if found, null otherwise
     */
    public StudioContent getContentById(UUID contentId) {
        return contentRepository.findById(contentId).orElse(null);
    }

    /**
     * Gets all content for a studio.
     *
     * @param studioId The studio ID
     * @return List of all content in the studio
     */
    @PreAuthorize("@studioMembershipService.canViewContent(#studioId)")
    public List<StudioContent> getStudioContent(UUID studioId) {
        return contentRepository.findByStudioId(studioId);
    }

    /**
     * Gets published content for a studio.
     *
     * @param studioId The studio ID
     * @return List of published content in the studio
     */
    public List<StudioContent> getPublishedStudioContent(UUID studioId) {
        return contentRepository.findByStudioIdAndIsPublishedTrue(studioId);
    }

    /**
     * Gets Studios Special content for a studio.
     *
     * @param studioId The studio ID
     * @return List of featured content in the studio's Studios Special collection
     */
    public List<StudioContent> getStudiosSpecialContent(UUID studioId) {
        return contentRepository.findByStudioIdAndIsPublishedTrueAndIsSpecialTrue(studioId);
    }

    /**
     * Updates content information.
     *
     * @param contentId The content ID
     * @param title The new title
     * @param description The new description
     * @param contentCategory The new content category
     * @param tags The new tags
     * @param isSpecial Whether to feature this content in Studios Special
     * @return The updated content
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioContentService.getStudioIdByContentId(#contentId))")
    public StudioContent updateContent(UUID contentId, String title, String description,
                                       String contentCategory, String tags, boolean isSpecial) {
        StudioContent content = getContentById(contentId);
        if (content == null) {
            throw new IllegalArgumentException("Content not found: " + contentId);
        }

        if (title != null && !title.trim().isEmpty()) {
            content.setTitle(title.trim());
        }
        if (description != null) {
            content.setDescription(description);
        }
        if (contentCategory != null) {
            content.setContentCategory(contentCategory);
        }
        if (tags != null) {
            content.setTags(tags);
        }
        content.setSpecial(isSpecial);
        content.setUpdatedAt(Instant.now());

        StudioContent updatedContent = contentRepository.save(content);
        log.info("Content updated: {} in studio {}", contentId, content.getStudioId());
        return updatedContent;
    }

    /**
     * Toggles whether content is featured in Studios Special.
     *
     * @param contentId The content ID
     * @return The updated content
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioContentService.getStudioIdByContentId(#contentId))")
    public StudioContent toggleSpecialContent(UUID contentId) {
        StudioContent content = getContentById(contentId);
        if (content == null) {
            throw new IllegalArgumentException("Content not found: " + contentId);
        }

        content.setSpecial(!content.isSpecial());
        content.setUpdatedAt(Instant.now());

        StudioContent updatedContent = contentRepository.save(content);
        log.info("Content special status toggled: {} in studio {} to {}",
                contentId, content.getStudioId(), content.isSpecial());
        return updatedContent;
    }

    /**
     * Reorders content in a studio.
     *
     * @param studioId The studio ID
     * @param contentIds The content IDs in the new order
     */
    @PreAuthorize("@studioMembershipService.canManageContent(#studioId)")
    public void reorderContent(UUID studioId, List<UUID> contentIds) {
        // Verify studio exists
        var studio = studioRepository.findById(studioId);
        if (studio.isEmpty() || !studio.get().isActiveAndNotDeleted()) {
            throw new IllegalArgumentException("Studio not found or inactive: " + studioId);
        }

        // Get all content for the studio
        List<StudioContent> allContent = contentRepository.findByStudioId(studioId);

        // Update positions based on the new order
        for (int i = 0; i < contentIds.size(); i++) {
            UUID contentId = contentIds.get(i);
            StudioContent content = allContent.stream()
                    .filter(c -> c.getId().equals(contentId))
                    .findFirst()
                    .orElse(null);

            if (content != null) {
                content.setPosition(i);
                content.setUpdatedAt(Instant.now());
                contentRepository.save(content);
            }
        }

        log.info("Content reordered for studio {}", studioId);
    }

    /**
     * Removes content from a studio.
     *
     * @param contentId The content ID to remove
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioContentService.getStudioIdByContentId(#contentId))")
    public void removeContent(UUID contentId) {
        StudioContent content = getContentById(contentId);
        if (content == null) {
            throw new IllegalArgumentException("Content not found: " + contentId);
        }

        contentRepository.delete(content);
        log.info("Content removed from studio {}: {}", content.getStudioId(), contentId);
    }

    /**
     * Searches content within a studio by title or description.
     *
     * @param studioId The studio ID
     * @param searchTerm The search term
     * @return List of matching content
     */
    @PreAuthorize("@studioMembershipService.canViewContent(#studioId)")
    public List<StudioContent> searchContent(UUID studioId, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getStudioContent(studioId);
        }
        return contentRepository.searchByTitleOrDescription(studioId, searchTerm.trim());
    }

    /**
     * Gets the current user's ID from the authentication context.
     *
     * @return The current user's ID
     */
    protected UUID getCurrentUserId() {
        return currentUserAccessor.getCurrentUserId();
    }

    /** Resolves the parent studio for authorization of content-ID operations. */
    public UUID getStudioIdByContentId(UUID contentId) {
        if (contentId == null) return null;
        StudioContent content = getContentById(contentId);
        return content == null ? null : content.getStudioId();
    }
}
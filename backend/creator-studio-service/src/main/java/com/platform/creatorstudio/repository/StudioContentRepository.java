package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.StudioContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StudioContent entity.
 * Provides data access operations for studio content.
 */
@Repository
public interface StudioContentRepository extends JpaRepository<StudioContent, UUID> {

    /** Find content by studio ID */
    List<StudioContent> findByStudioId(UUID studioId);

    /** Find published content by studio ID */
    List<StudioContent> findByStudioIdAndIsPublishedTrue(UUID studioId);

    /** Find special content by studio ID */
    List<StudioContent> findByStudioIdAndIsSpecialTrue(UUID studioId);

    /** Find published special content by studio ID */
    List<StudioContent> findByStudioIdAndIsPublishedTrueAndIsSpecialTrue(UUID studioId);

    /** Find content by content reference ID */
    Optional<StudioContent> findByContentReferenceId(String contentReferenceId);

    /** Find content by studio ID and content type */
    List<StudioContent> findByStudioIdAndContentType(UUID studioId, String contentType);

    /** Search content by title or description */
    @Query("SELECT c FROM StudioContent c WHERE " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND c.studioId = :studioId")
    List<StudioContent> searchByTitleOrDescription(
            @Param("studioId") UUID studioId,
            @Param("search") String search);

    /** Get content ordered by position */
    List<StudioContent> findByStudioIdOrderByPositionAsc(UUID studioId);

    /** Get published content ordered by position */
    List<StudioContent> findByStudioIdAndIsPublishedTrueOrderByPositionAsc(UUID studioId);
}
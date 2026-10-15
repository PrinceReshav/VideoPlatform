package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.CreatorStudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CreatorStudio entity.
 * Provides data access operations for studio management.
 */
@Repository
public interface CreatorStudioRepository extends JpaRepository<CreatorStudio, UUID> {

    /** Find a studio by its unique name (case-insensitive) */
    Optional<CreatorStudio> findByNameIgnoreCase(String name);

    /** Find studios by owner ID */
    java.util.List<CreatorStudio> findByOwnerId(UUID ownerId);

    /** Find active studios by owner ID */
    java.util.List<CreatorStudio> findByOwnerIdAndIsActiveTrueAndDeletedFalse(UUID ownerId);

    /** Find studios by primary category */
    java.util.List<CreatorStudio> findByPrimaryCategory(com.platform.creatorstudio.model.StudioCategory category);

    /** Check if a studio name exists (case-insensitive) */
    boolean existsByNameIgnoreCase(String name);

    /** Find studios that are active and not deleted */
    java.util.List<CreatorStudio> findByIsActiveTrueAndDeletedFalse();

    /** Search studios by name or display name */
    @Query("SELECT c FROM CreatorStudio c WHERE " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.displayName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND c.isActive = true AND c.deleted = false")
    java.util.List<CreatorStudio> searchByNameOrDisplayName(@Param("search") String search);

    /** Get all studio names for Bloom Filter initialization */
    @Query("SELECT c.name FROM CreatorStudio c")
    List<String> findAllStudioNames();
}
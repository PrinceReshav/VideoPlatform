package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.StudioMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StudioMembership entity.
 * Provides data access operations for studio memberships.
 */
@Repository
public interface StudioMembershipRepository extends JpaRepository<StudioMembership, UUID> {

    /** Find membership by studio ID and user ID */
    Optional<StudioMembership> findByStudioIdAndUserId(UUID studioId, UUID userId);

    /** Find active memberships for a user */
    List<StudioMembership> findByUserIdAndIsActiveTrue(UUID userId);

    /** Find active members of a studio */
    List<StudioMembership> findByStudioIdAndIsActiveTrue(UUID studioId);

    /** Find members of a studio by role */
    List<StudioMembership> findByStudioIdAndRole(UUID studioId, com.platform.creatorstudio.model.StudioMembership.StudioRole role);

    long countByStudioIdAndRoleAndIsActiveTrue(
            UUID studioId,
            StudioMembership.StudioRole role
    );

    /** Check if user is a member of a studio */
    boolean existsByStudioIdAndUserIdAndIsActiveTrue(UUID studioId, UUID userId);

    /** Get the role of a user in a studio */
    Optional<StudioMembership.StudioRole> findRoleByStudioIdAndUserIdAndIsActiveTrue(UUID studioId, UUID userId);

    /** Find memberships that need to be cleaned up (invitations that expired) */
    @Query("SELECT m FROM StudioMembership m WHERE m.isActive = false AND m.updatedAt < :cutoff")
    List<StudioMembership> findInactiveMembershipsBefore(@Param("cutoff") java.time.Instant cutoff);
}
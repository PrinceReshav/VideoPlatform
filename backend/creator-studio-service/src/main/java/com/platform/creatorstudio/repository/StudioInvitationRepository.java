package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.StudioInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StudioInvitation entity.
 * Provides data access operations for studio invitations.
 */
@Repository
public interface StudioInvitationRepository extends JpaRepository<StudioInvitation, UUID> {

    /** Find invitation by token */
    Optional<StudioInvitation> findByInvitationToken(String invitationToken);

    /** Find pending invitations by email */
    List<StudioInvitation> findByEmailAndIsAcceptedFalse(String email);

    /** Find invitations sent by a studio */
    List<StudioInvitation> findByStudioId(UUID studioId);

    /** Find pending invitations for a studio */
    List<StudioInvitation> findByStudioIdAndIsAcceptedFalse(UUID studioId);

    /** Find expired invitations */
    @Query("SELECT i FROM StudioInvitation i WHERE i.expiresAt < :now AND i.isAccepted = false")
    List<StudioInvitation> findExpiredInvitations(@Param("now") java.time.Instant now);

    /** Find invitations that can be accepted by a user (by user ID or email) */
    @Query("SELECT i FROM StudioInvitation i WHERE " +
           "(i.userId = :userId OR LOWER(i.email) = LOWER(:email)) " +
           "AND i.isAccepted = false AND i.expiresAt > :now")
    List<StudioInvitation> findAcceptableInvitations(
            @Param("userId") UUID userId,
            @Param("email") String email,
            @Param("now") java.time.Instant now);

    /** Check if an invitation token exists */
    boolean existsByInvitationToken(String invitationToken);
}
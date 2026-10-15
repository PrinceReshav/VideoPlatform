package com.platform.user.repository;

import com.platform.user.entity.UserRelationship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserRelationship entity (Feature 26: Block/Mute/Restrict)
 */
@Repository
public interface UserRelationshipRepository extends JpaRepository<UserRelationship, UUID> {

    /** Find relationships where the user is the initiator (blocker, muter, etc.) */
    List<UserRelationship> findByFromUserId(UUID fromUserId);

    /** Find relationships where the user is the target (blocked, muted, etc.) */
    List<UserRelationship> findByToUserId(UUID toUserId);

    /** Find relationship between two users by type */
    Optional<UserRelationship> findByFromUserIdAndToUserIdAndRelationshipType(UUID fromUserId, UUID toUserId, String relationshipType);

    /** Find active relationships where the user is the initiator */
    List<UserRelationship> findByFromUserIdAndActiveTrue(UUID fromUserId);

    /** Find active relationships where the user is the target */
    List<UserRelationship> findByToUserIdAndActiveTrue(UUID toUserId);

    /** Find relationships by type with pagination */
    Page<UserRelationship> findByRelationshipType(String relationshipType, Pageable pageable);

    List<UserRelationship> findByFromUserIdAndRelationshipType(
            UUID fromUserId,
            String relationshipType
    );

    List<UserRelationship> findByToUserIdAndRelationshipType(
            UUID toUserId,
            String relationshipType
    );
}
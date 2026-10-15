package com.platform.common.repository;

import com.platform.common.entity.ABTestParticipant;
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
 * Repository for ABTestParticipant entity (Feature 30: A/B Testing)
 */
@Repository
public interface ABTestParticipantRepository extends JpaRepository<ABTestParticipant, UUID> {

    /** Find participants for a specific A/B test */
    List<ABTestParticipant> findByAbTestId(UUID abTestId);

    /** Find active participants for a specific A/B test */
    List<ABTestParticipant> findByAbTestIdAndActiveTrue(UUID abTestId);

    /** Find participants assigned to a specific variation */
    List<ABTestParticipant> findByAbTestVariationId(UUID abTestVariationId);

    /** Find a user's participation in a specific test */
    Optional<ABTestParticipant> findByUserIdAndAbTestId(UUID userId, UUID abTestId);

    /** Find participants by user with pagination */
    Page<ABTestParticipant> findByUserId(UUID userId, Pageable pageable);

    /** Count participants in a variation */
    Long countByAbTestVariationId(UUID abTestVariationId);
}
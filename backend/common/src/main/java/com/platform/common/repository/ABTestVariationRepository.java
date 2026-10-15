package com.platform.common.repository;

import com.platform.common.entity.ABTestVariation;
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
 * Repository for ABTestVariation entity (Feature 30: A/B Testing)
 */
@Repository
public interface ABTestVariationRepository extends JpaRepository<ABTestVariation, UUID> {

    /** Find variations for a specific A/B test */
    List<ABTestVariation> findByAbTestId(UUID abTestId);

    /** Find active variations for a specific A/B test */
    List<ABTestVariation> findByAbTestIdAndActiveTrue(UUID abTestId);

    /** Find the control variation for an A/B test */
    Optional<ABTestVariation> findByAbTestIdAndIsControlTrue(UUID abTestId);

    /** Find variations by name within a test */
    Optional<ABTestVariation> findByAbTestIdAndName(UUID abTestId, String name);

    /** Find variations with pagination */
    Page<ABTestVariation> findByAbTestId(UUID abTestId, Pageable pageable);
}
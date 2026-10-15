package com.platform.common.repository;

import com.platform.common.entity.ABTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ABTest entity (Feature 30: A/B Testing)
 */
@Repository
public interface ABTestRepository extends JpaRepository<ABTest, UUID> {

    /** Find active A/B tests */
    List<ABTest> findByActiveTrue();

    /** Find A/B tests by feature area */
    List<ABTest> findByFeatureArea(String featureArea);

    /** Find A/B test by name */
    Optional<ABTest> findByName(String name);

    /** Find A/B tests that are currently running (within start/end times) */
    @Query("SELECT t FROM ABTest t WHERE " +
           "t.active = true AND " +
           "(:now IS NULL OR t.startTime IS NULL OR t.startTime <= :now) AND " +
           "(:now IS NULL OR t.endTime IS NULL OR t.endTime >= :now)")
    List<ABTest> findRunningTests(@Param("now") Instant now);

    /** Find A/B tests with pagination */
    Page<ABTest> findAll(Pageable pageable);
}
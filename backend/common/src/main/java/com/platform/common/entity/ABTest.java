package com.platform.common.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Entity for A/B tests (Feature 30: A/B Testing)
 * Defines tests that compare different variations of features or content
 */
@Entity
@Table(name = "ab_tests", schema = "common_schema",
        indexes = {
                @Index(name = "idx_ab_tests_name", columnList = "name"),
                @Index(name = "idx_ab_tests_active", columnList = "active")
        })
@Getter
@Setter
public class ABTest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Unique name for the test */
    @Column(name = "name", nullable = false, length = 200, unique = true)
    private String name;

    /** Description of what the test is evaluating */
    @Column(name = "description", length = 1000)
    private String description;

    /** The feature or area being tested (e.g., "homepage_layout", "recommendation_algorithm") */
    @Column(name = "feature_area", nullable = false, length = 100)
    private String featureArea;

    /** Whether the test is currently active */
    @Column(name = "active", nullable = false)
    private boolean active = false;

    /** Start time of the test */
    @Column(name = "start_time")
    private Instant startTime;

    /** End time of the test */
    @Column(name = "end_time")
    private Instant endTime;

    /** The percentage of users to include in the test (0-100) */
    @Column(name = "traffic_percentage", nullable = false)
    private Integer trafficPercentage = 100;

    /** The date when the test results were last calculated */
    @Column(name = "last_calculated_at")
    private Instant lastCalculatedAt;
}
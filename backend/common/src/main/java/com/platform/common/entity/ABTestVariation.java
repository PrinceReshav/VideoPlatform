package com.platform.common.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for A/B test variations (Feature 30: A/B Testing)
 * Represents the different variations being tested in an A/B test
 */
@Entity
@Table(name = "ab_test_variations", schema = "common_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ab_test_variations_test_name", columnNames = {"ab_test_id", "name"})
        },
        indexes = {
                @Index(name = "idx_ab_test_variations_test", columnList = "ab_test_id"),
                @Index(name = "idx_ab_test_variations_name", columnList = "name")
        })
@Getter
@Setter
public class ABTestVariation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The A/B test this variation belongs to */
    @Column(name = "ab_test_id", nullable = false)
    private UUID abTestId;

    /** Name of the variation (e.g., "control", "variant_a", "button_red") */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Description of what this variation entails */
    @Column(name = "description", length = 1000)
    private String description;

    /** Whether this variation is the control group */
    @Column(name = "is_control", nullable = false)
    private boolean isControl = false;

    /** The percentage of traffic allocated to this variation */
    @Column(name = "traffic_allocation", nullable = false)
    private Integer trafficAllocation;

    /** Configuration or settings for this variation (JSON format) */
    @Column(name = "configuration", columnDefinition = "jsonb")
    private String configuration;

    /** Whether this variation is currently active */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
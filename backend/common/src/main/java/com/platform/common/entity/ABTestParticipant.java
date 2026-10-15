package com.platform.common.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for A/B test participants (Feature 30: A/B Testing)
 * Tracks which users are assigned to which variations of an A/B test
 */
@Entity
@Table(name = "ab_test_participants", schema = "common_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ab_test_participants_user_test", columnNames = {"user_id", "ab_test_id"})
        },
        indexes = {
                @Index(name = "idx_ab_test_participants_user", columnList = "user_id"),
                @Index(name = "idx_ab_test_participants_test", columnList = "ab_test_id"),
                @Index(name = "idx_ab_test_participants_variation", columnList = "ab_test_variation_id"),
                @Index(name = "idx_ab_test_participants_assigned_at", columnList = "assigned_at")
        })
@Getter
@Setter
public class ABTestParticipant extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user participating in the test */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The A/B test the user is participating in */
    @Column(name = "ab_test_id", nullable = false)
    private UUID abTestId;

    /** The specific variation the user is assigned to */
    @Column(name = "ab_test_variation_id", nullable = false)
    private UUID abTestVariationId;

    /** When the user was assigned to this variation */
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    /** Whether the participant is still active in the test */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
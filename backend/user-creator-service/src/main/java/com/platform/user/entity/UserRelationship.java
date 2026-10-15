package com.platform.user.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for user relationships (Feature 26: Block/Mute/Restrict)
 * Tracks relationships between users such as blocking, muting, restricting
 */
@Entity
@Table(name = "user_relationships", schema = "user_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_relationships", columnNames = {"from_user_id", "to_user_id", "relationship_type"})
        },
        indexes = {
                @Index(name = "idx_user_relationships_from_user", columnList = "from_user_id"),
                @Index(name = "idx_user_relationships_to_user", columnList = "to_user_id"),
                @Index(name = "idx_user_relationships_type", columnList = "relationship_type"),
                @Index(name = "idx_user_relationships_created_at", columnList = "created_at")
        })
@Getter
@Setter
public class UserRelationship extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user who initiated the relationship (blocker, muter, etc.) */
    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    /** The user who is the target of the relationship (blocked, muted, etc.) */
    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    /** Type of relationship: BLOCK, MUTE, RESTRICT */
    @Column(name = "relationship_type", nullable = false, length = 20)
    private String relationshipType; // BLOCK, MUTE, RESTRICT

    /** Additional notes or reason for the relationship */
    @Column(name = "notes", length = 500)
    private String notes;

    /** Whether the relationship is active */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
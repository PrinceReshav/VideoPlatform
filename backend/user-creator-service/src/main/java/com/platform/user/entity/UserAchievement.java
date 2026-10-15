package com.platform.user.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for user achievements (Feature 29: Creator Achievements)
 * Tracks which achievements users have earned
 */
@Entity
@Table(name = "user_achievements", schema = "user_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_achievements_user_achievement", columnNames = {"user_id", "achievement_definition_id"})
        },
        indexes = {
                @Index(name = "idx_user_achievements_user", columnList = "user_id"),
                @Index(name = "idx_user_achievements_achievement", columnList = "achievement_definition_id"),
                @Index(name = "idx_user_achievements_earned_at", columnList = "earned_at")
        })
@Getter
@Setter
public class UserAchievement extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user who earned the achievement */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The achievement definition that was earned */
    @Column(name = "achievement_definition_id", nullable = false)
    private UUID achievementDefinitionId;

    /** When the achievement was earned */
    @Column(name = "earned_at", nullable = false, updatable = false)
    private Instant earnedAt = Instant.now();

    /** The value at which the achievement was earned (for historical tracking) */
    @Column(name = "value_when_earned")
    private long valueWhenEarned;
}
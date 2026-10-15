package com.platform.user.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for achievement definitions (Feature 29: Creator Achievements)
 * Defines the achievements that creators can earn
 */
@Entity
@Table(name = "achievement_definitions", schema = "user_schema",
        indexes = {
                @Index(name = "idx_achievement_definitions_category", columnList = "category"),
                @Index(name = "idx_achievement_definitions_type", columnList = "achievement_type")
        })
@Getter
@Setter
public class AchievementDefinition extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Unique code for the achievement (e.g., "SUBSCRIBER_100", "VIEW_COUNT_1000") */
    @Column(name = "achievement_code", nullable = false, length = 100, unique = true)
    private String achievementCode;

    /** Human-readable name of the achievement */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Description of what the achievement represents */
    @Column(name = "description", length = 1000)
    private String description;

    /** Category of achievement: SUBSCRIBERS, VIEWS, VIDEOS, LIKES, COMMENTS, etc. */
    @Column(name = "category", nullable = false, length = 50)
    private String category; // SUBSCRIBERS, VIEWS, VIDEOS, LIKES, COMMENTS, etc.

    /** Type of metric: COUNT, TOTAL, RATE */
    @Column(name = "achievement_type", nullable = false, length = 20)
    private String achievementType; // COUNT, TOTAL, RATE

    /** Threshold value that must be reached to earn this achievement */
    @Column(name = "threshold_value", nullable = false)
    private long thresholdValue;

    /** Points or reward value associated with this achievement */
    @Column(name = "points")
    private Integer points;

    /** Whether this achievement is currently active and can be earned */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Icon or image associated with the achievement */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /** Color associated with the achievement for display purposes */
    @Column(name = "color", length = 20)
    private String color;
}
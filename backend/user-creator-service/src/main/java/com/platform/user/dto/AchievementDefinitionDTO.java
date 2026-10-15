package com.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for achievement definitions (Feature 29: Creator Achievements)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDefinitionDTO {
    private UUID id;
    private String achievementCode;
    private String name;
    private String description;
    private String category;
    private String achievementType;
    private long thresholdValue;
    private Integer points;
    private boolean active;
    private String iconUrl;
    private String color;
    private Instant createdAt;
    private Instant updatedAt;
}
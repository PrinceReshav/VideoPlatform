package com.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user achievements (Feature 29: Creator Achievements)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievementDTO {
    private UUID id;
    private UUID userId;
    private UUID achievementDefinitionId;
    private String achievementCode;
    private String achievementName;
    private String achievementDescription;
    private String achievementCategory;
    private long valueWhenEarned;
    private Instant earnedAt;
}
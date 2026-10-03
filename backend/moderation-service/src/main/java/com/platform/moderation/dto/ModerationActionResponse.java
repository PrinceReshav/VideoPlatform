package com.platform.moderation.dto;

import com.platform.moderation.entity.ModerationAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationActionResponse {
    private UUID id;
    private UUID reportId;
    private UUID moderatorId;
    private String moderatorUsername;
    private ModerationAction.TargetType targetType;
    private UUID targetId;
    private ModerationAction.ActionType actionType;
    private String reason;
    private Integer durationHours;
    private Instant expiresAt;
    private Instant createdAt;
}
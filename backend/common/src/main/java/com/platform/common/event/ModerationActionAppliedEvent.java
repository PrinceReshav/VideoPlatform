package com.platform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ModerationActionAppliedEvent {
    private UUID reportId;
    private UUID moderatorId;
    private String targetType;
    private UUID targetId;
    private String actionType;
    private String reason;
    private Integer durationHours;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant expiresAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}

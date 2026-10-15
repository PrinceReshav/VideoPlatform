package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for A/B test participants (Feature 30: A/B Testing)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABTestParticipantDTO {
    private UUID id;
    private UUID userId;
    private UUID abTestId;
    private UUID abTestVariationId;
    private Instant assignedAt;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
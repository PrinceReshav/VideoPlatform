package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for A/B test variations (Feature 30: A/B Testing)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABTestVariationDTO {
    private UUID id;
    private UUID abTestId;
    private String name;
    private String description;
    private boolean isControl;
    private Integer trafficAllocation;
    private String configuration;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
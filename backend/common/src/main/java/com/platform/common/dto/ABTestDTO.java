package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for A/B tests (Feature 30: A/B Testing)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABTestDTO {
    private UUID id;
    private String name;
    private String description;
    private String featureArea;
    private boolean active;
    private Instant startTime;
    private Instant endTime;
    private Integer trafficPercentage;
    private Instant lastCalculatedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
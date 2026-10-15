package com.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user relationship responses (Feature 26: Block/Mute/Restrict)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRelationshipResponse {
    private UUID id;
    private UUID fromUserId;
    private UUID toUserId;
    private String relationshipType; // BLOCK, MUTE, RESTRICT
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
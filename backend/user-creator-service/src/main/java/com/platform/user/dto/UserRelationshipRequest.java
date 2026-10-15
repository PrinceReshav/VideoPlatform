package com.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for user relationship requests (Feature 26: Block/Mute/Restrict)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRelationshipRequest {
    @NotNull(message = "Target user ID is required")
    private UUID targetUserId;

    @NotBlank(message = "Relationship type is required")
    private String relationshipType; // BLOCK, MUTE, RESTRICT

    private String notes;
}
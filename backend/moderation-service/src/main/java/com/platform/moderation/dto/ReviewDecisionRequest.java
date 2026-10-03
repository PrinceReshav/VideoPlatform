package com.platform.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionRequest {
    @NotNull(message = "Decision is required")
    private Decision decision;

    @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
    private String reviewNotes;

    @Size(max = 50, message = "Action taken must not exceed 50 characters")
    private String actionTaken;

    private Integer durationHours; // For temporary actions

    public enum Decision {
        APPROVE, // Content is fine, dismiss report
        REJECT, // Take action on content
        ESCALATE // Escalate to higher authority
    }
}
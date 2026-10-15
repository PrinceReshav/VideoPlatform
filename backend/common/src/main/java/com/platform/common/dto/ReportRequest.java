package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for report requests (Feature 18: Reporting/Strikes/Trust System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    /** The user who made the report */
    @NotNull
    private UUID reporterId;

    /** The target being reported (video ID, channel ID, user ID, comment ID, etc.) */
    @NotNull
    private UUID targetId;

    /** The type of target being reported */
    @NotBlank
    @Size(max = 50)
    private String targetType; // VIDEO, CHANNEL, USER, COMMENT, PLAYLIST, etc.

    /** The reason for the report */
    @NotBlank
    @Size(max = 100)
    private String reason; // HARASSMENT, HATE_SPEECH, VIOLENCE, MISINFORMATION, COPYRIGHT, PRIVACY, SPAM, etc.

    /** Detailed description of the issue */
    @Size(max = 2000)
    private String description;

    /** Additional evidence or context (JSON format) */
    private String evidence;
}
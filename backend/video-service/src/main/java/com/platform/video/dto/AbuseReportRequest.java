package com.platform.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for creating abuse reports (Feature 33: Anti-Bot/Anti-Abuse System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbuseReportRequest {

    /** The user being reported (if applicable) */
    private UUID reportedUserId;

    /** The video being reported (if applicable) */
    private UUID videoId;

    /** The comment being reported (if applicable) */
    private UUID commentId;

    /** The live stream being reported (if applicable) */
    private UUID liveStreamId;

    /** The type of abuse being reported */
    @NotBlank(message = "Abuse type is required")
    @Size(max = 50, message = "Abuse type must not exceed 50 characters")
    private String abuseType;

    /** Detailed description of the abuse */
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
}
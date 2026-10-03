package com.platform.moderation.dto;

import com.platform.moderation.entity.Report;
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
public class ReportResponse {
    private UUID id;
    private UUID reporterId;
    private String reporterUsername;
    private Report.TargetType targetType;
    private UUID targetId;
    private String targetTitle;
    private Report.Reason reason;
    private String description;
    private Report.Status status;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private String reviewNotes;
    private String actionTaken;
    private Instant createdAt;
    private Instant updatedAt;
}
package com.platform.moderation.dto;

import com.platform.moderation.entity.Report;
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
public class CreateReportRequest {
    @NotNull(message = "Target type is required")
    private Report.TargetType targetType;

    @NotNull(message = "Target ID is required")
    private UUID targetId;

    @NotNull(message = "Reason is required")
    private Report.Reason reason;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}
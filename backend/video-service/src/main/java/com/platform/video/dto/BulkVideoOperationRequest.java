package com.platform.video.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk video operations (Feature 7: Video Manager)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkVideoOperationRequest {

    /** List of video IDs to operate on */
    @NotNull
    private List<UUID> videoIds;

    /** Operation type: PUBLISH, UNPUBLISH, DELETE, RESTORE, UPDATE_TAGS, UPDATE_VISIBILITY */
    @NotNull
    private String operation;

    /** Parameters for the operation */
    private String tags; // For UPDATE_TAGS operation
    private String visibility; // For UPDATE_VISIBILITY operation
}
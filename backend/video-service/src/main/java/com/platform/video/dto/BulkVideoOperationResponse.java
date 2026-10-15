package com.platform.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk video operation responses (Feature 7: Video Manager)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkVideoOperationResponse {

    /** Number of videos successfully processed */
    private int successfulCount;

    /** Number of videos that failed to process */
    private int failedCount;

    /** List of video IDs that were successfully processed */
    private List<UUID> successfulVideoIds;

    /** List of video IDs that failed to process */
    private List<UUID> failedVideoIds;

    /** Error messages for failed operations */
    private List<String> errorMessages;
}
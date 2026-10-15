package com.platform.video.dto;

import com.platform.video.entity.OfflineDownload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for offline download responses (Feature 34: Offline Downloads with DRM)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineDownloadResponse {

    /** The offline download ID */
    private UUID id;

    /** The user who downloaded the video */
    private UUID userId;

    /** The video that was downloaded */
    private UUID videoId;

    /** When the download expires */
    private Instant expiresAt;

    /** Current status of the offline download */
    private OfflineDownload.OfflineDownloadStatus status;

    /** Whether the download has been played */
    private boolean hasBeenPlayed;

    /** When the download was last played */
    private Instant lastPlayedAt;

    /** Number of times the download has been played */
    private int playCount;

    /** DRM license identifier */
    private String drmLicenseId;

    /** Local file path or identifier for the downloaded content */
    private String localIdentifier;

    /** Size of the downloaded file in bytes */
    private Long fileSizeBytes;

    /** When the offline download was created */
    private Instant createdAt;

    /** When the offline download was last updated */
    private Instant updatedAt;

    /**
     * Convert OfflineDownload entity to OfflineDownloadResponse DTO
     */
    public static OfflineDownloadResponse fromEntity(OfflineDownload offlineDownload) {
        return OfflineDownloadResponse.builder()
                .id(offlineDownload.getId())
                .userId(offlineDownload.getUserId())
                .videoId(offlineDownload.getVideoId())
                .expiresAt(offlineDownload.getExpiresAt())
                .status(offlineDownload.getStatus())
                .hasBeenPlayed(offlineDownload.isHasBeenPlayed())
                .lastPlayedAt(offlineDownload.getLastPlayedAt())
                .playCount(offlineDownload.getPlayCount())
                .drmLicenseId(offlineDownload.getDrmLicenseId())
                .localIdentifier(offlineDownload.getLocalIdentifier())
                .fileSizeBytes(offlineDownload.getFileSizeBytes())
                .createdAt(offlineDownload.getCreatedAt())
                .updatedAt(offlineDownload.getUpdatedAt())
                .build();
    }
}
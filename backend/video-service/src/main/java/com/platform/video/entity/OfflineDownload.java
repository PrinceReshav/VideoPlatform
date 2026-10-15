package com.platform.video.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking offline downloads (Feature 34: Offline Downloads with DRM)
 * Tracks when users download videos for offline viewing
 */
@Entity
@Table(name = "offline_downloads", schema = "video_schema",
        indexes = {
                @Index(name = "idx_offline_downloads_user", columnList = "user_id"),
                @Index(name = "idx_offline_downloads_video", columnList = "video_id"),
                @Index(name = "idx_offline_downloads_expires_at", columnList = "expires_at"),
                @Index(name = "idx_offline_downloads_status", columnList = "status")
        })
@Getter
@Setter
public class OfflineDownload extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user who downloaded the video */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The video that was downloaded */
    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    /** When the download expires (based on license/rental terms) */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Current status of the offline download */
    @Column(name = "status", nullable = false, length = 20)
    private OfflineDownloadStatus status = OfflineDownloadStatus.ACTIVE;

    /** Whether the download has been played */
    @Column(name = "has_been_played", nullable = false)
    private boolean hasBeenPlayed = false;

    /** When the download was last played */
    @Column(name = "last_played_at")
    private Instant lastPlayedAt;

    /** Number of times the download has been played */
    @Column(name = "play_count", nullable = false)
    private int playCount = 0;

    /** DRM license identifier (would be used with actual DRM system) */
    @Column(name = "drm_license_id", length = 255)
    private String drmLicenseId;

    /** Local file path or identifier for the downloaded content */
    @Column(name = "local_identifier", length = 500)
    private String localIdentifier;

    /** Size of the downloaded file in bytes */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    public enum OfflineDownloadStatus {
        ACTIVE, EXPIRED, REVOKED, DELETED
    }
}
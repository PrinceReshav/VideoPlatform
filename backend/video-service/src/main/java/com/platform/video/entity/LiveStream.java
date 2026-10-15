package com.platform.video.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for live streams (Feature 31: Live Streaming Platform)
 * Tracks live streams that users are broadcasting
 */
@Entity
@Table(name = "live_streams", schema = "video_schema",
        indexes = {
                @Index(name = "idx_live_streams_creator", columnList = "creator_id"),
                @Index(name = "idx_live_streams_channel", columnList = "channel_id"),
                @Index(name = "idx_live_stream_status", columnList = "status"),
                @Index(name = "idx_live_streams_started_at", columnList = "started_at")
        })
@Getter
@Setter
public class LiveStream extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user who owns this live stream */
    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    /** The channel associated with this live stream */
    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    /** Title of the live stream */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** Description of the live stream */
    @Column(name = "description", length = 5000)
    private String description;

    /** Unique stream key for broadcasting (like RTMP key) */
    @Column(name = "stream_key", nullable = false, length = 100, unique = true)
    private String streamKey;

    /** Current status of the live stream */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LiveStreamStatus status = LiveStreamStatus.OFFLINE;

    /** When the live stream started */
    @Column(name = "started_at")
    private Instant startedAt;

    /** When the live stream ended */
    @Column(name = "ended_at")
    private Instant endedAt;

    /** Preview/thumbnail URL for the live stream */
    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    /** Whether the live stream should be saved as VOD after ending */
    @Column(name = "save_as_vod", nullable = false)
    private boolean saveAsVod = true;

    /** The VOD video ID if this live stream was saved as a video */
    @Column(name = "vod_video_id")
    private UUID vodVideoId;

    /** Current viewer count */
    @Column(name = "viewer_count", nullable = false)
    private int viewerCount = 0;

    /** Total views/hours watched */
    @Column(name = "total_views")
    private Long totalViews = 0L;

    public enum LiveStreamStatus {
        OFFLINE, ONLINE, STARTING, ENDING, ERROR
    }
}
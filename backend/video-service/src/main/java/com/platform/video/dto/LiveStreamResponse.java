package com.platform.video.dto;

import com.platform.video.entity.LiveStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for live stream responses (Feature 31: Live Streaming Platform)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStreamResponse {

    /** The live stream ID */
    private UUID id;

    /** The user who owns this live stream */
    private UUID creatorId;

    /** The channel associated with this live stream */
    private UUID channelId;

    /** Title of the live stream */
    private String title;

    /** Description of the live stream */
    private String description;

    /** Unique stream key for broadcasting (like RTMP key) */
    private String streamKey;

    /** Current status of the live stream */
    private LiveStream.LiveStreamStatus status;

    /** When the live stream started */
    private Instant startedAt;

    /** When the live stream ended */
    private Instant endedAt;

    /** Preview/thumbnail URL for the live stream */
    private String previewUrl;

    /** Whether the live stream should be saved as VOD after ending */
    private boolean saveAsVod;

    /** The VOD video ID if this live stream was saved as a video */
    private UUID vodVideoId;

    /** Current viewer count */
    private int viewerCount;

    /** Total views/hours watched */
    private Long totalViews;

    /** When the live stream was created */
    private Instant createdAt;

    /** When the live stream was last updated */
    private Instant updatedAt;

    /**
     * Convert LiveStream entity to LiveStreamResponse DTO
     */
    public static LiveStreamResponse fromEntity(LiveStream liveStream) {
        return LiveStreamResponse.builder()
                .id(liveStream.getId())
                .creatorId(liveStream.getCreatorId())
                .channelId(liveStream.getChannelId())
                .title(liveStream.getTitle())
                .description(liveStream.getDescription())
                .streamKey(liveStream.getStreamKey())
                .status(liveStream.getStatus())
                .startedAt(liveStream.getStartedAt())
                .endedAt(liveStream.getEndedAt())
                .previewUrl(liveStream.getPreviewUrl())
                .saveAsVod(liveStream.isSaveAsVod())
                .vodVideoId(liveStream.getVodVideoId())
                .viewerCount(liveStream.getViewerCount())
                .totalViews(liveStream.getTotalViews())
                .createdAt(liveStream.getCreatedAt())
                .updatedAt(liveStream.getUpdatedAt())
                .build();
    }
}
package com.platform.engagement.dto;

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
public class WatchHistoryResponse {
    private UUID id;
    private UUID videoId;
    private String videoTitle;
    private String videoThumbnailUrl;
    private String channelName;
    private String channelSlug;
    private int positionSeconds;
    private int durationSeconds;
    private int progressPercent;
    private boolean completed;
    private Instant lastWatchedAt;
    private int watchCount;
}
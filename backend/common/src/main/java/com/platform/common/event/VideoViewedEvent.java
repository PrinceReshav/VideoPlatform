package com.platform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class VideoViewedEvent {
    private UUID videoId;
    private UUID channelId;
    private UUID userId;
    private Integer positionSeconds;
    private Integer durationSeconds;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}
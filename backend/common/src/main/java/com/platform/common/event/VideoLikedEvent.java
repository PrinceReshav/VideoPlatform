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
public class VideoLikedEvent {
    private UUID videoId;
    private UUID channelId;
    private UUID userId;
    private boolean liked;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}
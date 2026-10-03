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
public class VideoDeletedEvent {
    private UUID videoId;
    private UUID deletedBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant deletedAt;
}
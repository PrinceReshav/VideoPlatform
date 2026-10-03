package com.platform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingStartedEvent {
    private UUID videoId;
    private UUID jobId;
    private String sourceKey;
    private List<String> resolutions;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startedAt;
}
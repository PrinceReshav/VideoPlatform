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
public class ProcessingCompletedEvent {
    private UUID videoId;
    private UUID jobId;
    private String hlsManifestKey;
    private Set<String> resolutions;
    private Integer durationSeconds;
    private String thumbnailUrl;
    private List<com.platform.common.entity.VideoFileInfo> videoFiles;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant completedAt;
}
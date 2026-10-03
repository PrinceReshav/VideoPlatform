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
public class ProcessingFailedEvent {
    private UUID videoId;
    private UUID jobId;
    private String errorCode;
    private String errorMessage;
    private boolean retryable;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant failedAt;
}
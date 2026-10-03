package com.platform.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishRequest {
    private boolean scheduled = false;
    private java.time.Instant scheduledAt;
}
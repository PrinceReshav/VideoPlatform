package com.platform.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResolutionInfo {
    private String resolution;
    private String width;
    private String height;
    private Long bitrate;
    private String codec;
    private String manifestUrl;
}

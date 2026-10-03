package com.platform.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFileInfo {
    private UUID videoId;
    private FileType fileType;
    private String resolution;
    private String objectKey;
    private String bucket;
    private Long fileSize;
    private String contentType;
    private String checksum;
    private Integer durationSeconds;
    private Integer bitrate;
    private Integer width;
    private Integer height;
    private String codec;
    private Integer segmentIndex;
    private boolean master;
}
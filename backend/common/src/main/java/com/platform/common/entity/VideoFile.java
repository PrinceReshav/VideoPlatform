package com.platform.common.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_files", schema = "video_schema",
        indexes = {
                @Index(name = "idx_video_files_video", columnList = "video_id"),
                @Index(name = "idx_video_files_type", columnList = "file_type"),
                @Index(name = "idx_video_files_resolution", columnList = "resolution")
        })
@Getter
@Setter
public class VideoFile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @Column(name = "resolution", length = 20)
    private String resolution;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "bucket", nullable = false, length = 100)
    private String bucket;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "bitrate")
    private Integer bitrate;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "codec", length = 50)
    private String codec;

    @Column(name = "segment_index")
    private Integer segmentIndex;

    @Column(name = "is_master", nullable = false)
    private boolean master = false;

}

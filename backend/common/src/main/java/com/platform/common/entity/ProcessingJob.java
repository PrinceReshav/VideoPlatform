package com.platform.common.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_jobs", schema = "video_schema",
        indexes = {
                @Index(name = "idx_processing_jobs_video", columnList = "video_id"),
                @Index(name = "idx_processing_jobs_status", columnList = "status"),
                @Index(name = "idx_processing_jobs_worker", columnList = "worker_id")
        })
@Getter
@Setter
public class ProcessingJob extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Column(name = "source_object_key", length = 500)
    private String sourceObjectKey;

    @Column(name = "source_bucket", length = 100)
    private String sourceBucket;

    @Column(name = "output_bucket", length = 100)
    private String outputBucket;

    @Column(name = "output_prefix", length = 200)
    private String outputPrefix;

    @Column(name = "resolutions", length = 500)
    private String resolutions; // JSON array

    @Column(name = "hls_segment_duration")
    private int hlsSegmentDuration = 6;

    @Column(name = "progress")
    private int progress = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    public enum Status {
        PENDING, QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
}
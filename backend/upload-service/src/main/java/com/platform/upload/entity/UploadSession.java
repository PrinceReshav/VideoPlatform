package com.platform.upload.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_sessions", schema = "upload_schema",
        indexes = {
                @Index(name = "idx_upload_sessions_user", columnList = "user_id"),
                @Index(name = "idx_upload_sessions_status", columnList = "status"),
                @Index(name = "idx_upload_sessions_expires", columnList = "expires_at"),
                @Index(name = "idx_upload_sessions_video", columnList = "video_id")
        })
@Getter
@Setter
public class UploadSession extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id")
    private UUID videoId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "total_size", nullable = false)
    private Long totalSize;

    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Column(name = "uploaded_chunks", nullable = false)
    private Integer uploadedChunks = 0;

    @Column(name = "checksum", length = 64)
    private String checksum; // SHA-256 of complete file

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.INITIATED;

    @Column(name = "minio_upload_id", length = 255)
    private String minioUploadId;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "bucket", length = 100)
    private String bucket;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    public enum Status {
        INITIATED, IN_PROGRESS, COMPLETED, FAILED, EXPIRED, CANCELLED
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public double getProgressPercent() {
        if (totalChunks == 0) return 0;
        return (double) uploadedChunks / totalChunks * 100;
    }
}
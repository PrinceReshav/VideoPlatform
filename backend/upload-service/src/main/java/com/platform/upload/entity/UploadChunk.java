package com.platform.upload.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_chunks", schema = "upload_schema",
        indexes = {
                @Index(name = "idx_upload_chunks_session", columnList = "session_id"),
                @Index(name = "idx_upload_chunks_part", columnList = "session_id, part_number")
        })
@Getter
@Setter
public class UploadChunk extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "part_number", nullable = false)
    private Integer partNumber;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "etag", length = 64)
    private String etag;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "minio_part_number")
    private Integer minioPartNumber;
}
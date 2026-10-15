package com.platform.user.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for verification requests (Feature 19: Creator Verification)
 * Tracks requests for channel/creator profile verification
 */
@Entity
@Table(name = "verification_requests", schema = "user_schema",
        indexes = {
                @Index(name = "idx_verification_requests_user", columnList = "user_id"),
                @Index(name = "idx_verification_requests_status", columnList = "status"),
                @Index(name = "idx_verification_requests_type", columnList = "request_type")
        })
@Getter
@Setter
public class VerificationRequest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user making the verification request */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Type of verification requested: CHANNEL, CREATOR_PROFILE, BOTH */
    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType; // CHANNEL, CREATOR_PROFILE, BOTH

    /** Current status of the verification request */
    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    /** Verification tier being requested: BASIC, VERIFIED, OFFICIAL */
    @Column(name = "requested_tier", length = 20)
    private String requestedTier; // BASIC, VERIFIED, OFFICIAL

    /** Reason for requesting verification */
    @Column(name = "request_reason", length = 1000)
    private String requestReason;

    /** Additional information or documentation URLs (JSON format) */
    @Column(name = "additional_info", columnDefinition = "jsonb")
    private String additionalInfo;

    /** Admin/moderator who reviewed the request */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    /** Timestamp when the request was reviewed */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Feedback or reason for rejection */
    @Column(name = "review_feedback", length = 1000)
    private String reviewFeedback;

    /** Whether the request has been processed (approved or rejected) */
    @Column(name = "processed", nullable = false)
    private boolean processed = false;
}
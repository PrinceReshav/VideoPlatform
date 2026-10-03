package com.platform.moderation.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports", schema = "moderation_schema",
        indexes = {
                @Index(name = "idx_reports_reporter", columnList = "reporter_id"),
                @Index(name = "idx_reports_target", columnList = "target_type, target_id"),
                @Index(name = "idx_reports_status", columnList = "status"),
                @Index(name = "idx_reports_created_at", columnList = "created_at")
        })
@Getter
@Setter
public class Report extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private Reason reason;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;

    @Column(name = "action_taken", length = 50)
    private String actionTaken;

    public enum TargetType {
        VIDEO, COMMENT, CHANNEL, USER, PLAYLIST
    }

    public enum Reason {
        SPAM,
        HATE_SPEECH,
        HARASSMENT,
        VIOLENCE,
        SEXUAL_CONTENT,
        CHILD_SAFETY,
        COPYRIGHT,
        MISLEADING,
        PRIVACY,
        SELF_HARM,
        ILLEGAL_ACTIVITY,
        IMPERSONATION,
        OTHER
    }

    public enum Status {
        PENDING,
        UNDER_REVIEW,
        RESOLVED,
        DISMISSED,
        ESCALATED
    }
}
package com.platform.moderation.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moderation_actions", schema = "moderation_schema",
        indexes = {
                @Index(name = "idx_moderation_actions_report", columnList = "report_id"),
                @Index(name = "idx_moderation_actions_moderator", columnList = "moderator_id"),
                @Index(name = "idx_moderation_actions_target", columnList = "target_type, target_id"),
                @Index(name = "idx_moderation_actions_created_at", columnList = "created_at")
        })
@Getter
@Setter
public class ModerationAction extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "moderator_id", nullable = false)
    private UUID moderatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "duration_hours")
    private Integer durationHours; // For temporary actions

    @Column(name = "expires_at")
    private Instant expiresAt;

    public enum TargetType {
        VIDEO, COMMENT, CHANNEL, USER, PLAYLIST
    }

    public enum ActionType {
        WARN,
        HIDE_CONTENT,
        DELETE_CONTENT,
        TEMPORARY_SUSPENSION,
        PERMANENT_SUSPENSION,
        CHANNEL_STRIKE,
        CHANNEL_TERMINATION,
        REMOVE_STRIKE,
        RESTORE_CONTENT
    }
}
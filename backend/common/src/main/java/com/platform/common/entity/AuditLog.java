package com.platform.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log entity for tracking security and administrative actions (Feature 38: Security & Audit Logs)
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE audit_logs SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    /** The user who performed the action (null for system actions) */
    private UUID userId;

    /** The type of action performed */
    @Column(length = 50, nullable = false)
    private String actionType;

    /** The entity type that was acted upon */
    @Column(length = 50)
    private String entityType;

    /** The ID of the entity that was acted upon */
    private UUID entityId;

    /** Description of the action */
    @Column(length = 500)
    private String description;

    /** IP address of the user who performed the action */
    @Column(length = 45)
    private String ipAddress;

    /** User agent of the user who performed the action */
    @Column(length = 255)
    private String userAgent;

    /** Whether the action was successful */
    private boolean success = true;

    /** Error message if the action failed */
    @Column(length = 500)
    private String errorMessage;

    /** Additional metadata stored as JSON */
    @Column(columnDefinition = "jsonb")
    private String metadata;

    /** Timestamp when the action occurred */
    private Instant timestamp;

    /** Soft delete flag */
    private boolean deleted = false;
}
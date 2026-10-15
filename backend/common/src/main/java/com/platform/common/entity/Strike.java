package com.platform.common.entity;

import com.platform.common.audit.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Strike entity for Feature 18: Reporting/Strikes/Trust System.
 * Represents a strike against a user or channel for policy violations.
 */
@Entity
@Table(
        name = "strikes",
        indexes = {
                @Index(
                        name = "idx_strikes_target_created",
                        columnList = "target_id, target_type, created_at"
                ),
                @Index(
                        name = "idx_strikes_expires",
                        columnList = "expires_at"
                ),
                @Index(
                        name = "idx_strikes_active",
                        columnList = "is_active"
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SQLDelete(sql = "UPDATE strikes SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Strike extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    /** The target receiving the strike. */
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /** USER, CHANNEL, CREATOR_PROFILE, etc. */
    @Column(name = "target_type", length = 50, nullable = false)
    private String targetType;

    /** Reason for the strike. */
    @Column(name = "reason", length = 100, nullable = false)
    private String reason;

    /** Details about the violation. */
    @Column(name = "details", length = 2000)
    private String details;

    /** Whether the strike is currently active. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Timestamp when the strike was issued. */
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    /** Timestamp when the strike expires; null means permanent. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Moderator who issued the strike. */
    @Column(name = "issued_by")
    private UUID issuedBy;

    /** Reference to the originating report, if applicable. */
    @Column(name = "report_id")
    private UUID reportId;
}
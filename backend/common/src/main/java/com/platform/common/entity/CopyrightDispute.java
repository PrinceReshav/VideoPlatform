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
 * Copyright dispute entity for Feature 3: Copyright Detection & Claims System
 * Represents a dispute filed against a copyright claim
 */
@Entity
@Table(name = "copyright_disputes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE copyright_disputes SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class CopyrightDispute {

    @Id
    @GeneratedValue
    private UUID id;

    /** The copyright claim being disputed */
    private UUID copyrightClaimId;

    /** The user filing the dispute (usually the video creator/uploader) */
    private UUID disputantId;

    /** The reason for the dispute */
    @Column(length = 500)
    private String disputeReason;

    /** Explanation or evidence supporting the dispute */
    @Column(columnDefinition = "text")
    private String explanation;

    /** Type of dispute */
    @Column(length = 50)
    private String disputeType; // FAIR_USE, NOT_INFRINGING, LICENSED, PUBLIC_DOMAIN, etc.

    /** Status of the dispute */
    @Column(length = 20, nullable = false)
    private String status; // PENDING, UNDER_REVIEW, ACCEPTED, REJECTED, WITHDRAWN

    /** Action taken on the copyright claim due to the dispute */
    @Column(length = 20)
    private String actionTaken; // NONE, CLAIM_RELEASED, CLAIM_UPHELD, SETTLED

    /** Whether the dispute has been legally reviewed */
    private boolean legallyReviewed = false;

    /** Evidence provided by the disputant */
    @Column(columnDefinition = "jsonb")
    private String evidence;

    /** Response from the claimant */
    @Column(columnDefinition = "jsonb")
    private String claimantResponse;

    /** Timestamp when the dispute was filed */
    private Instant filedAt;

    /** Timestamp when the dispute was last updated */
    private Instant updatedAt;

    /** Timestamp when the dispute expires (if applicable) */
    private Instant expiresAt;

    /** Soft delete flag */
    private boolean deleted = false;
}
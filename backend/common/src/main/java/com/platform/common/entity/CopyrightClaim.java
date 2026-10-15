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
 * Copyright claim entity for Feature 3: Copyright Detection & Claims System
 */
@Entity
@Table(name = "copyright_claims")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE copyright_claims SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class CopyrightClaim {

    @Id
    @GeneratedValue
    private UUID id;

    /** The video that allegedly infringes copyright */
    private UUID videoId;

    /** The user who filed the copyright claim */
    private UUID claimantId;

    /** The copyright holder or rights owner */
    private UUID rightsOwnerId;

    /** Description of the copyrighted work */
    @Column(length = 500)
    private String copyrightedWorkDescription;

    /** Registration number or identifier for the copyrighted work */
    @Column(length = 100)
    private String copyrightRegistrationNumber;

    /** URL or reference to the original copyrighted work */
    @Column(length = 500)
    private String originalWorkUrl;

    /** Timestamp when the alleged infringement occurs in the video (in seconds) */
    private Integer infringementTimestamp;

    /** Duration of the alleged infringement in the video (in seconds) */
    private Integer infringementDuration;

    /** Type of copyright infringement */
    @Column(length = 50)
    private String infringementType; // VIDEO, AUDIO, IMAGE, TEXT, etc.

    /** Status of the copyright claim */
    @Column(length = 20, nullable = false)
    private String status; // PENDING, VALIDATED, REJECTED, WITHDRAWN, SETTLED

    /** Action taken on the video due to the claim */
    @Column(length = 20)
    private String actionTaken; // NONE, MONETIZE, BLOCK, TRACK, TAKE_DOWN

    /** Whether the claim has been legally validated */
    private boolean legallyValidated = false;

    /** Evidence provided by the claimant */
    @Column(columnDefinition = "jsonb")
    private String evidence;

    /** Response from the video uploader/creator */
    @Column(columnDefinition = "jsonb")
    private String creatorResponse;

    /** Timestamp when the claim was filed */
    private Instant filedAt;

    /** Timestamp when the claim was last updated */
    private Instant updatedAt;

    /** Timestamp when the claim expires (if applicable) */
    private Instant expiresAt;

    /** Soft delete flag */
    private boolean deleted = false;
}
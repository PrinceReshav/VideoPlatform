package com.platform.user.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "creator_profiles", schema = "user_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_creator_profiles_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_creator_profiles_channel_name", columnNames = "channel_name")
        },
        indexes = {
                @Index(name = "idx_creator_profiles_user", columnList = "user_id"),
                @Index(name = "idx_creator_profiles_channel_name", columnList = "channel_name"),
                @Index(name = "idx_creator_profiles_verified", columnList = "verified"),
                @Index(name = "idx_creator_profiles_active", columnList = "active")
        })
@Getter
@Setter
public class CreatorProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "channel_name", nullable = false, length = 100)
    private String channelName;

    @Column(name = "channel_slug", nullable = false, length = 120)
    private String channelSlug;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "verification_document_url", length = 500)
    private String verificationDocumentUrl;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "subscriber_count", nullable = false)
    private long subscriberCount = 0;

    @Column(name = "video_count", nullable = false)
    private int videoCount = 0;

    @Column(name = "total_views", nullable = false)
    private long totalViews = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "suspended", nullable = false)
    private boolean suspended = false;

    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_by")
    private UUID suspendedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "creator_genres",
            schema = "user_schema",
            joinColumns = @JoinColumn(name = "creator_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new java.util.HashSet<>();

    @Column(name = "default_language_id")
    private UUID defaultLanguageId;

    @Column(name = "monetization_enabled", nullable = false)
    private boolean monetizationEnabled = false;

    @Column(name = "monetization_approved_at")
    private Instant monetizationApprovedAt;

    @Column(name = "payout_email", length = 255)
    private String payoutEmail;

    @Column(name = "custom_url", length = 100)
    private String customUrl;

    @Column(name = "keywords", length = 1000)
    private String keywords;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "email", length = 255)
    private String email;
}
package com.platform.user.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "channels", schema = "user_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_channels_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_channels_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uk_channels_creator", columnNames = "creator_profile_id")
        },
        indexes = {
                @Index(name = "idx_channels_creator", columnList = "creator_profile_id"),
                @Index(name = "idx_channels_slug", columnList = "slug"),
                @Index(name = "idx_channels_active", columnList = "active"),
                @Index(name = "idx_channels_verified", columnList = "verified")
        })
@Getter
@Setter
public class Channel extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "creator_profile_id", nullable = false, unique = true, updatable = false)
    private UUID creatorProfileId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "verification_document_url", length = 500)
    private String verificationDocumentUrl;

    @Column(name = "verified_at")
    private java.time.Instant verifiedAt;

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
    private java.time.Instant suspendedAt;

    @Column(name = "suspended_by")
    private UUID suspendedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "channel_genres",
            schema = "user_schema",
            joinColumns = @JoinColumn(name = "channel_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @Column(name = "default_language_id")
    private UUID defaultLanguageId;

    @Column(name = "custom_url", length = 100)
    private String customUrl;

    @Column(name = "keywords", length = 1000)
    private String keywords;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "email", length = 255)
    private String email;
}
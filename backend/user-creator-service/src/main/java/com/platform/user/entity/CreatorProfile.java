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

    /** Enhanced creator profile features for Feature 15: Creator Channels/Profiles */
    /** Whether the profile is listed in search and recommendations */
    @Column(name = "is_listed", nullable = false)
    private boolean isListed = true;

    /** Whether the creator allows fan funding */
    @Column(name = "allows_fan_funding", nullable = false)
    private boolean allowsFanFunding = false;

    /** Whether the creator allows merchandise sales */
    @Column(name = "allows_merchandise", nullable = false)
    private boolean allowsMerchandise = false;

    /** Whether the creator allows sponsorships */
    @Column(name = "allows_sponsorships", nullable = false)
    private boolean allowsSponsorships = false;

    /** Whether the creator allows affiliate links */
    @Column(name = "allows_affiliate_links", nullable = false)
    private boolean allowsAffiliateLinks = false;

    /** Default maturity rating for new content */
    @Column(name = "default_maturity_rating", length = 20)
    private String defaultMaturityRating = "GENERAL"; // GENERAL, MATURE, RESTRICTED

    /** Whether to show subscriber count publicly */
    @Column(name = "show_subscriber_count", nullable = false)
    private boolean showSubscriberCount = true;

    /** Keywords/tags for the creator (comma-separated) */
    @Column(name = "creator_keywords", length = 1000)
    private String creatorKeywords;

    /** LinkedIn profile URL */
    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    /** Twitter/X handle */
    @Column(name = "twitter_handle", length = 100)
    private String twitterHandle;

    /** Instagram handle */
    @Column(name = "instagram_handle", length = 100)
    private String instagramHandle;

    /** TikTok handle */
    @Column(name = "tiktok_handle", length = 100)
    private String tiktokHandle;

    /** Discord invite URL */
    @Column(name = "discord_url", length = 500)
    private String discordUrl;

    /** Patreon URL */
    @Column(name = "patreon_url", length = 500)
    private String patreonUrl;

    /** Ko-fi URL */
    @Column(name = "ko_fi_url", length = 500)
    private String koFiUrl;

    /** Buy Me a Coffee URL */
    @Column(name = "buy_me_coffee_url", length = 500)
    private String buyMeCoffeeUrl;

    /** Whether the creator is open to collaborations */
    @Column(name = "open_to_collaborations", nullable = false)
    private boolean openToCollaborations = false;

    /** Whether the creator is open to sponsorships */
    @Column(name = "open_to_sponsorships", nullable = false)
    private boolean openToSponsorships = false;

    /** Primary content language */
    @Column(name = "primary_language_id")
    private UUID primaryLanguageId;

    /** Secondary content languages (comma-separated language IDs) */
    @Column(name = "secondary_languages", length = 500)
    private String secondaryLanguages;

    /** Content niche/category (comma-separated category IDs) */
    @Column(name = "content_niche", length = 500)
    private String contentNiche;

    /** Upload schedule (cron expression or human-readable) */
    @Column(name = "upload_schedule", length = 100)
    private String uploadSchedule;

    /** Whether to send email notifications for new uploads */
    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    /** Whether to allow content ID matching */
    @Column(name = "allow_content_id_matching", nullable = false)
    private boolean allowContentIdMatching = true;

    /** Whether to allow automated copyright claims */
    @Column(name = "allow_automated_claims", nullable = false)
    private boolean allowAutomatedClaims = true;
}
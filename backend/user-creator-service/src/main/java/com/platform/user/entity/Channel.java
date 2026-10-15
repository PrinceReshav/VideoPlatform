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

    /** Enhanced channel features for Feature 15: Creator Channels/Profiles */
    /** Whether the channel is listed in search and recommendations */
    @Column(name = "is_listed", nullable = false)
    private boolean isListed = true;

    /** Whether the channel allows community posts */
    @Column(name = "allows_community", nullable = false)
    private boolean allowsCommunity = true;

    /** Whether the channel allows messaging from subscribers */
    @Column(name = "allows_messaging", nullable = false)
    private boolean allowsMessaging = false;

    /** Default privacy setting for new videos */
    @Column(name = "default_video_privacy", length = 20)
    private String defaultVideoPrivacy = "PUBLIC"; // PUBLIC, UNLISTED, PRIVATE

    /** Default license for new videos */
    @Column(name = "default_license", length = 50)
    private String defaultLicense = "STANDARD_YOUTUBE"; // STANDARD_YOUTUBE, CREATIVE_COMMONS

    /** Whether to show subscriber count publicly */
    @Column(name = "show_subscriber_count", nullable = false)
    private boolean showSubscriberCount = true;

    /** Whether to show like/dislike counts publicly */
    @Column(name = "show_rating_counts", nullable = false)
    private boolean showRatingCounts = true;

    /** Verification tier/level */
    @Column(name = "verification_tier", length = 20)
    private String verificationTier = "NONE"; // NONE, BASIC, VERIFIED, OFFICIAL

    /** Keywords/tags for the channel (comma-separated) */
    @Column(name = "channel_keywords", length = 1000)
    private String channelKeywords;

    /** Associated AdSense account ID */
    @Column(name = "adsense_account_id", length = 50)
    private String adsenseAccountId;

    /** Google Analytics property ID */
    @Column(name = "google_analytics_id", length = 50)
    private String googleAnalyticsId;

    /** Channel trailer video ID */
    @Column(name = "trailer_video_id")
    private UUID trailerVideoId;

    /** Featured channels (comma-separated video IDs) */
    @Column(name = "featured_channels", length = 1000)
    private String featuredChannels;

    /** Featured playlists (comma-separated playlist IDs) */
    @Column(name = "featured_playlists", length = 1000)
    private String featuredPlaylists;

    /** Country restriction settings */
    @Column(name = "country_restriction", length = 100)
    private String countryRestriction; // comma-separated country codes, empty = worldwide

    /** Age restriction setting */
    @Column(name = "age_restriction", length = 20)
    private String ageRestriction = "NONE"; // NONE, RESTRICTED

    /** Made for kids setting */
    @Column(name = "made_for_kids", nullable = false)
    private boolean madeForKids = false;

    /** Automatic chapter generation enabled */
    @Column(name = "auto_chapters_enabled", nullable = false)
    private boolean autoChaptersEnabled = false;

    /** Automatic captions enabled */
    @Column(name = "auto_captions_enabled", nullable = false)
    private boolean autoCaptionsEnabled = true;

    /** Default comment settings for new videos */
    @Column(name = "default_comment_setting", length = 20)
    private String defaultCommentSetting = "ALLOW_ALL"; // ALLOW_ALL, HOLD_FOR_REVIEW, HOLD_POTENTIALLY_HARMFUL, DISABLE_COMMENTS

    public void setCategoryId(UUID categoryId) {
        if (categoryId == null) {
            this.category = null;
            return;
        }

        Category category = new Category();
        category.setId(categoryId);
        this.category = category;
    }

}
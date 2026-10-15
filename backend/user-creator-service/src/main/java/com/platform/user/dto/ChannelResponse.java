package com.platform.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponse {

    private UUID id;
    private UUID creatorProfileId;
    private String name;
    private String slug;
    private String description;
    private String avatarUrl;
    private String bannerUrl;
    private boolean verified;
    private Instant verifiedAt;
    private long subscriberCount;
    private int videoCount;
    private long totalViews;
    private boolean active;
    private boolean suspended;
    private String suspensionReason;
    private UUID categoryId;
    private String categoryName;
    private Set<UUID> genreIds;
    private Set<String> genreNames;
    private UUID defaultLanguageId;
    private String defaultLanguageName;
    private String customUrl;
    private String keywords;
    private String country;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;

    @JsonProperty("isListed")
    private boolean listed;

    private boolean allowsCommunity;
    private boolean allowsMessaging;
    private String defaultVideoPrivacy;
    private String defaultLicense;
    private boolean showSubscriberCount;
    private boolean showRatingCounts;
    private String verificationTier;
    private String channelKeywords;
    private String adsenseAccountId;
    private String googleAnalyticsId;
    private UUID trailerVideoId;
    private String featuredChannels;
    private String featuredPlaylists;
    private String countryRestriction;
    private String ageRestriction;
    private boolean madeForKids;
    private boolean autoChaptersEnabled;
    private boolean autoCaptionsEnabled;
    private String defaultCommentSetting;
}
package com.platform.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for updating a channel (Feature 15: Creator Channels/Profiles)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChannelRequest {

    @Size(min = 2, max = 100, message = "Channel name must be 2-100 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private UUID creatorProfileId;

    private UUID categoryId;

    private Set<UUID> genreIds;

    private UUID defaultLanguageId;

    private String customUrl;

    private String keywords;

    private String country;

    private String email;

    /** Enhanced channel features for Feature 15: Creator Channels/Profiles */
    private Boolean isListed;
    private Boolean allowsCommunity;
    private Boolean allowsMessaging;
    private String defaultVideoPrivacy;
    private String defaultLicense;
    private Boolean showSubscriberCount;
    private Boolean showRatingCounts;
    private String verificationTier;
    private String channelKeywords;
    private String adsenseAccountId;
    private String googleAnalyticsId;
    private UUID trailerVideoId;
    private String featuredChannels;
    private String featuredPlaylists;
    private String countryRestriction;
    private String ageRestriction;
    private Boolean madeForKids;
    private Boolean autoChaptersEnabled;
    private Boolean autoCaptionsEnabled;
    private String defaultCommentSetting;
}
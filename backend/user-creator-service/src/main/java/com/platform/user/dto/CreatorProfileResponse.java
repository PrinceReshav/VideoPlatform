package com.platform.user.dto;

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
public class CreatorProfileResponse {

    private UUID id;
    private UUID userId;
    private String channelName;
    private String channelSlug;
    private String description;
    private String avatarUrl;
    private String bannerUrl;
    private String websiteUrl;
    private String location;
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

    /** Enhanced creator profile features for Feature 15: Creator Channels/Profiles */
    private boolean isListed;
    private boolean allowsFanFunding;
    private boolean allowsMerchandise;
    private boolean allowsSponsorships;
    private boolean allowsAffiliateLinks;
    private String defaultMaturityRating;
    private boolean showSubscriberCount;
    private String creatorKeywords;
    private String linkedinUrl;
    private String twitterHandle;
    private String instagramHandle;
    private String tiktokHandle;
    private String discordUrl;
    private String patreonUrl;
    private String koFiUrl;
    private String buyMeCoffeeUrl;
    private boolean openToCollaborations;
    private boolean openToSponsorships;
    private UUID primaryLanguageId;
    private String secondaryLanguages;
    private String contentNiche;
    private String uploadSchedule;
    private boolean emailNotificationsEnabled;
    private boolean allowContentIdMatching;
    private boolean allowAutomatedClaims;
}
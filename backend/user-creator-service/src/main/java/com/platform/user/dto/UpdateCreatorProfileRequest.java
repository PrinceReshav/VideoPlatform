package com.platform.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCreatorProfileRequest {

    @Size(min = 2, max = 100, message = "Channel name must be 2-100 characters")
    private String channelName;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private UUID categoryId;

    private Set<UUID> genreIds;

    private UUID defaultLanguageId;

    private String websiteUrl;

    private String location;

    private String customUrl;

    private String keywords;

    private String country;

    private String email;

    private String avatarUrl;

    private String bannerUrl;

    /** Enhanced creator profile features for Feature 15: Creator Channels/Profiles */
    private Boolean isListed;
    private Boolean allowsFanFunding;
    private Boolean allowsMerchandise;
    private Boolean allowsSponsorships;
    private Boolean allowsAffiliateLinks;
    private String defaultMaturityRating;
    private Boolean showSubscriberCount;
    private String creatorKeywords;
    private String linkedinUrl;
    private String twitterHandle;
    private String instagramHandle;
    private String tiktokHandle;
    private String discordUrl;
    private String patreonUrl;
    private String koFiUrl;
    private String buyMeCoffeeUrl;
    private Boolean openToCollaborations;
    private Boolean openToSponsorships;
    private UUID primaryLanguageId;
    private String secondaryLanguages;
    private String contentNiche;
    private String uploadSchedule;
    private Boolean emailNotificationsEnabled;
    private Boolean allowContentIdMatching;
    private Boolean allowAutomatedClaims;
}
package com.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateCreatorProfileRequest {

    @NotBlank(message = "Channel name is required")
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
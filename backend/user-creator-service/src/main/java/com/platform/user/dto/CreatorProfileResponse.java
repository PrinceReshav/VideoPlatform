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
}
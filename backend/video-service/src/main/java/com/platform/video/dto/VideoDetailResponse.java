package com.platform.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDetailResponse {

    private UUID id;
    private UUID creatorId;
    private UUID channelId;
    private String title;
    private String slug;
    private String description;
    private String tags;
    private UUID genreId;
    private String genreName;
    private UUID categoryId;
    private String categoryName;
    private UUID languageId;
    private String languageName;
    private Integer durationSeconds;
    private String thumbnailUrl;
    private String previewUrl;
    private String visibility;
    private String processingState;
    private String publicationState;
    private Instant publishedAt;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Boolean allowComments;
    private Boolean allowEmbedding;
    private Boolean ageRestricted;
    private Boolean blocked;
    private String blockedReason;
    private Set<String> availableResolutions;
    private String hlsManifestUrl;
    private List<VideoResolutionInfo> resolutionDetails;
    private Instant createdAt;
    private Instant updatedAt;
    private String creatorName;
    private String channelName;
    private String channelSlug;
    private String channelAvatarUrl;
    private boolean isSubscribed;
    private boolean isLiked;
    private Long watchProgress; // seconds
}

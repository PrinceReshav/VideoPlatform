package com.platform.search.dto;

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
public class VideoSearchResult {

    private String videoId;
    private String slug;
    private String title;
    private String description;
    private String creatorId;
    private String channelId;
    private String channelName;
    private Set<String> tags;
    private String genreId;
    private String genreName;
    private String categoryId;
    private String categoryName;
    private String languageId;
    private String languageName;
    private Integer durationSeconds;
    private Long viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Instant publishedAt;
    private String thumbnailUrl;
    private String visibility;
    private String processingState;
    private Boolean isPublished;
    private Float score;
}
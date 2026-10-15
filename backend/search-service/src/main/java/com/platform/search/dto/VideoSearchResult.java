package com.platform.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enhanced Video Search Result for Feature 10: Advanced Elasticsearch Search
 * Includes additional fields for advanced search capabilities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSearchResult {

    private String videoId;
    private String slug;
    private String title;
    private String description;
    /** Transcript/searchable text from video audio */
    private String transcript;
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
    /** Geographic data for location-based search */
    private List<Double> location; // [lat, lon]
    private String countryCode;
    private String regionCode;
    private String cityName;
    private Instant publishedAt;
    private String thumbnailUrl;
    private String visibility;
    private String processingState;
    private Boolean isPublished;
    /** Enhanced engagement metrics */
    private Double engagementScore;
    private Integer watchTimeSeconds;
    private Integer shares;
    private Integer subscribersGained;
    private Float score;
}
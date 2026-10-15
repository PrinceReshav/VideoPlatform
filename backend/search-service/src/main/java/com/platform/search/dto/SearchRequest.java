package com.platform.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Enhanced Search Request for Feature 10: Advanced Elasticsearch Search
 * Adds support for geographic search, transcript search, and advanced filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    private String q;

    private UUID genreId;

    private UUID categoryId;

    private UUID creatorId;

    private UUID channelId;

    private UUID languageId;

    private Boolean isPublished;

    private List<String> visibility;

    private String processingState;

    private Integer durationMin;

    private Integer durationMax;

    private String sort = "relevance"; // relevance, publishedAt, viewCount, likeCount

    private String publishedAfter;

    private String publishedBefore;

    /** Geographic search parameters */
    private Double latitude; // For geo-distance search
    private Double longitude; // For geo-distance search
    private String distance; // e.g., "10km", "5mi"
    private String countryCode; // Filter by country
    private String regionCode; // Filter by region/state
    private String cityName; // Filter by city

    /** Transcript search */
    private Boolean searchTranscript; // Whether to search within video transcripts

    /** Advanced filtering */
    private Long minViewCount; // Minimum view count
    private Long maxViewCount; // Maximum view count
    private Integer minDuration; // Minimum duration in seconds
    private Integer maxDuration; // Maximum duration in seconds
    private Boolean hasTranscript; // Filter videos that have transcripts
    private Boolean isLive; // Filter live videos
    private Boolean isUpcoming; // Filter upcoming scheduled videos

    @Min(1)
    @Max(100)
    private int size = 20;

    private String cursor;
}
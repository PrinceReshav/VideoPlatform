package com.platform.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    @Min(1)
    @Max(100)
    private int size = 20;

    private String cursor;
}
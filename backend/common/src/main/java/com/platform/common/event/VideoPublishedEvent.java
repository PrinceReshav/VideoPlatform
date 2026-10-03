package com.platform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class VideoPublishedEvent {
    private UUID videoId;
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
    private String visibility;
    private Set<String> availableResolutions;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant publishedAt;
    private String channelName;
    private String channelSlug;
}
package com.platform.video.projection;

import com.platform.common.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class VideoListProjection {
    private final UUID id;
    private final String title;
    private final String slug;
    private final String thumbnailUrl;
    private final Integer durationSeconds;
    private final long viewCount;
    private final long likeCount;
    private final long commentCount;
    private final Instant publishedAt;
    private final String visibility;
    private final String processingState;
    private final String publicationState;
    private final UUID categoryId;
    private final UUID genreId;
    private final UUID languageId;
    private final String categoryName;
    private final String genreName;
    private final String channelName;
}

package com.platform.video.dto;

import com.platform.common.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for trending video responses (Feature 27: Trending/Velocity System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingVideoResponse {

    /** The video ID */
    private UUID id;

    /** The video title */
    private String title;

    /** The video thumbnail URL */
    private String thumbnailUrl;

    /** The video creator ID */
    private UUID creatorId;

    /** Current view count */
    private Long viewCount;

    /** Current like count */
    private Long likeCount;

    /** Current comment count */
    private Long commentCount;

    /** Current share count */
    private Long shareCount;

    /** Trending score based on velocity */
    private Double trendingScore;

    /** Rank in the trending list */
    private Integer rank;

    /** When the video was published */
    private Instant publishedAt;

    /** When the trending score was last calculated */
    private Instant lastCalculatedAt;
}
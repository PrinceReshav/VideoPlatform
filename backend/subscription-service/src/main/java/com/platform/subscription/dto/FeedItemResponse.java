package com.platform.subscription.dto;

import com.platform.common.dto.CursorPageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemResponse {
    private UUID videoId;
    private String title;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private String channelName;
    private String channelSlug;
    private String channelAvatarUrl;
    private Long viewCount;
    private Instant publishedAt;
}
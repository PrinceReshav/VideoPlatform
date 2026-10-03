package com.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelAnalyticsSummary {
    private Long totalViews;
    private Long totalWatchTimeSeconds;
    private Long totalLikes;
    private Long totalComments;
    private Long totalShares;
    private Long totalSubscribersGained;
    private Long totalSubscribersLost;
    private Long totalVideosPublished;
}
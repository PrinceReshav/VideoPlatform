package com.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalyticsResponse {
    private UUID videoId;
    private String videoTitle;
    private VideoAnalyticsSummary summary;
    private List<DailyVideoAnalytics> dailyData;

    // Additional fields for comprehensive analytics
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String periodType; // DAILY, WEEKLY, MONTHLY, CUSTOM
}
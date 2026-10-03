package com.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyVideoAnalytics {
    private LocalDate date;
    private long views;
    private long watchTimeSeconds;
    private long likes;
    private long comments;
    private long shares;
    private long subscribersGained;
    private long subscribersLost;
    private Double avgViewDurationSeconds;
    private Double avgViewPercentage;
}
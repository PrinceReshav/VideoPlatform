package com.platform.analytics.controller;

import com.platform.analytics.dto.ChannelAnalyticsResponse;
import com.platform.analytics.dto.VideoAnalyticsResponse;
import com.platform.analytics.service.AnalyticsService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/videos/{videoId}")
    public ResponseEntity<VideoAnalyticsResponse> getVideoAnalytics(
            @PathVariable UUID videoId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getVideoAnalytics(videoId, startDate, endDate));
    }

    @GetMapping("/channels/{channelId}")
    public ResponseEntity<ChannelAnalyticsResponse> getChannelAnalytics(
            @PathVariable UUID channelId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getChannelAnalytics(channelId, startDate, endDate));
    }
}
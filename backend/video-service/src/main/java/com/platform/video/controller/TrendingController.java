package com.platform.video.controller;

import com.platform.video.dto.TrendingVideoResponse;
import com.platform.video.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for trending/velocity calculations (Feature 27: Trending/Velocity System)
 */
@RestController
@RequestMapping("/api/v1/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    /**
     * Get trending videos based on velocity (Feature 27: Trending/Velocity System)
     */
    @GetMapping("/videos")
    public ResponseEntity<List<TrendingVideoResponse>> getTrendingVideos(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "24") int hoursLookback) {
        List<TrendingVideoResponse> trendingVideos = trendingService.getTrendingVideos(limit, hoursLookback);
        return ResponseEntity.ok(trendingVideos);
    }

    /**
     * Record current metrics for a video (typically called by a scheduled job)
     */
    @PostMapping("/videos/{videoId}/metrics")
    public ResponseEntity<Void> recordVideoMetrics(@PathVariable UUID videoId) {
        trendingService.recordVideoMetrics(videoId);
        return ResponseEntity.ok().build();
    }
}
package com.platform.engagement.controller;

import com.platform.engagement.service.RecommendationService;
import com.platform.video.dto.VideoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for video recommendations (Feature 28: Recommendation Engine)
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Get recommended videos for the current user
     */
    @GetMapping
    public ResponseEntity<List<VideoResponse>> getRecommendedVideos(
            @RequestParam(defaultValue = "10") int limit) {
        List<VideoResponse> recommendations = recommendationService.getRecommendedVideos(limit);
        return ResponseEntity.ok(recommendations);
    }
}
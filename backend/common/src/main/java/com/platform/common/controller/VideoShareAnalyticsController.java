package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.dto.VideoShareAnalyticsRequest;
import com.platform.common.dto.VideoShareAnalyticsResponse;
import com.platform.common.entity.VideoShareAnalytics;
import com.platform.common.service.VideoShareAnalyticsService;
import com.platform.common.util.IpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for video share analytics management (Feature 13: Privacy & Sharing Controls)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/video-share-analytics")
@RequiredArgsConstructor
public class VideoShareAnalyticsController {

    private final VideoShareAnalyticsService videoShareAnalyticsService;

    /**
     * Record an access to a video share link (typically called from the share link endpoint)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> recordShareAccess(
            @Valid @RequestBody VideoShareAnalyticsRequest request) {

        videoShareAnalyticsService.recordShareAccess(
            request.getShareLinkId(),
            request.getVideoId(),
            request.getCreatedBy(),
            null, // In a real implementation, we'd get the request from HttpServletRequest
            request.isSuccess(),
            request.getFailureReason());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get analytics for a share link
     */
    @GetMapping("/share-link/{shareLinkId}")
    public ResponseEntity<ApiResponse<List<VideoShareAnalyticsResponse>>> getAnalyticsByShareLinkId(
            @PathVariable UUID shareLinkId) {

        List<VideoShareAnalytics> analytics = videoShareAnalyticsService.getAnalyticsByShareLinkId(shareLinkId);
        List<VideoShareAnalyticsResponse> responses = analytics.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get analytics for a video
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<VideoShareAnalyticsResponse>>> getAnalyticsByVideoId(
            @PathVariable UUID videoId) {

        List<VideoShareAnalytics> analytics = videoShareAnalyticsService.getAnalyticsByVideoId(videoId);
        List<VideoShareAnalyticsResponse> responses = analytics.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get analytics by a creator
     */
    @GetMapping("/creator/{createdBy}")
    public ResponseEntity<ApiResponse<List<VideoShareAnalyticsResponse>>> getAnalyticsByCreatedBy(
            @PathVariable UUID createdBy) {

        List<VideoShareAnalytics> analytics = videoShareAnalyticsService.getAnalyticsByCreatedBy(createdBy);
        List<VideoShareAnalyticsResponse> responses = analytics.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get analytics by timestamp range
     */
    @GetMapping("/timestamp-range")
    public ResponseEntity<ApiResponse<List<VideoShareAnalyticsResponse>>> getAnalyticsByTimestampRange(
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {

        List<VideoShareAnalytics> analytics = videoShareAnalyticsService.getAnalyticsByTimestampRange(startTime, endTime);
        List<VideoShareAnalyticsResponse> responses = analytics.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get analytics with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoShareAnalyticsResponse>>> getAnalyticsByCriteria(
            @RequestParam(required = false) UUID shareLinkId,
            @RequestParam(required = false) UUID videoId,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // For simplicity, we'll get all matching results without pagination in this example
        // In a real implementation, you'd want to add pagination support to the service method
        List<VideoShareAnalytics> analytics = videoShareAnalyticsService.getAnalyticsByCriteria(
                shareLinkId, videoId, createdBy, startTime, endTime);
        List<VideoShareAnalyticsResponse> responses = analytics.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Map VideoShareAnalytics entity to VideoShareAnalyticsResponse DTO
     */
    private VideoShareAnalyticsResponse mapToResponse(VideoShareAnalytics analytics) {
        return VideoShareAnalyticsResponse.builder()
                .id(analytics.getId())
                .shareLinkId(analytics.getShareLinkId())
                .videoId(analytics.getVideoId())
                .createdBy(analytics.getCreatedBy())
                .accessedAt(analytics.getAccessedAt())
                .ipAddress(analytics.getIpAddress())
                .userAgent(analytics.getUserAgent())
                .referrerUrl(analytics.getReferrerUrl())
                .countryCode(analytics.getCountryCode())
                .success(analytics.isSuccess())
                .failureReason(analytics.getFailureReason())
                .build();
    }
}
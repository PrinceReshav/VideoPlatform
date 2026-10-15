package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.dto.VideoShareLinkRequest;
import com.platform.common.dto.VideoShareLinkResponse;
import com.platform.common.entity.VideoShareLink;
import com.platform.common.service.VideoShareLinkService;
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
 * Controller for video share link management (Feature 13: Privacy & Sharing Controls)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/video-share-links")
@RequiredArgsConstructor
public class VideoShareLinkController {

    private final VideoShareLinkService videoShareLinkService;

    /**
     * Create a new video share link
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VideoShareLinkResponse>> createVideoShareLink(
            @Valid @RequestBody VideoShareLinkRequest request) {

        VideoShareLink shareLink = VideoShareLink.builder()
                .videoId(request.getVideoId())
                .createdBy(request.getCreatedBy())
                .description(request.getDescription())
                .maxUses(request.getMaxUses())
                .passwordProtected(request.isPasswordProtected())
                .passwordHash(request.getPasswordHash())
                .expiresAt(request.getExpiresAt())
                .build();

        VideoShareLink savedShareLink = videoShareLinkService.createVideoShareLink(shareLink);
        VideoShareLinkResponse response = mapToResponse(savedShareLink);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get a video share link by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoShareLinkResponse>> getVideoShareLinkById(
            @PathVariable UUID id) {

        VideoShareLink shareLink = videoShareLinkService.getVideoShareLinkById(id);
        VideoShareLinkResponse response = mapToResponse(shareLink);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get a video share link by share token (public endpoint)
     */
    @GetMapping("/token/{shareToken}")
    public ResponseEntity<ApiResponse<VideoShareLinkResponse>> getVideoShareLinkByShareToken(
            @PathVariable String shareToken) {

        VideoShareLink shareLink = videoShareLinkService.getVideoShareLinkByShareToken(shareToken);
        VideoShareLinkResponse response = mapToResponse(shareLink);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all share links for a video
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<VideoShareLinkResponse>>> getVideoShareLinksByVideoId(
            @PathVariable UUID videoId) {

        List<VideoShareLink> shareLinks = videoShareLinkService.getVideoShareLinksByVideoId(videoId);
        List<VideoShareLinkResponse> responses = shareLinks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get all share links by a creator
     */
    @GetMapping("/creator/{createdBy}")
    public ResponseEntity<ApiResponse<List<VideoShareLinkResponse>>> getVideoShareLinksByCreatedBy(
            @PathVariable UUID createdBy) {

        List<VideoShareLink> shareLinks = videoShareLinkService.getVideoShareLinksByCreatedBy(createdBy);
        List<VideoShareLinkResponse> responses = shareLinks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Increment usage count for a share link (called when link is accessed)
     */
    @PostMapping("/{id}/usage")
    public ResponseEntity<ApiResponse<Void>> incrementUsage(
            @PathVariable UUID id) {

        videoShareLinkService.incrementUsage(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Update a video share link
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoShareLinkResponse>> updateVideoShareLink(
            @PathVariable UUID id,
            @Valid @RequestBody VideoShareLinkRequest request) {

        VideoShareLink shareLinkDetails = VideoShareLink.builder()
                .videoId(request.getVideoId())
                .createdBy(request.getCreatedBy())
                .description(request.getDescription())
                .maxUses(request.getMaxUses())
                .passwordProtected(request.isPasswordProtected())
                .passwordHash(request.getPasswordHash())
                .expiresAt(request.getExpiresAt())
                .build();

        VideoShareLink updatedShareLink = videoShareLinkService.updateVideoShareLink(id, shareLinkDetails);
        VideoShareLinkResponse response = mapToResponse(updatedShareLink);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a video share link
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideoShareLink(
            @PathVariable UUID id) {

        videoShareLinkService.deleteVideoShareLink(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get video share links with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoShareLinkResponse>>> getVideoShareLinksByCriteria(
            @RequestParam(required = false) UUID videoId,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) String shareToken,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // For simplicity, we'll get all matching results without pagination in this example
        // In a real implementation, you'd want to add pagination support to the service method
        List<VideoShareLink> shareLinks = videoShareLinkService.getVideoShareLinksByCriteria(
                videoId, createdBy, shareToken, startTime, endTime);
        List<VideoShareLinkResponse> responses = shareLinks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Map VideoShareLink entity to VideoShareLinkResponse DTO
     */
    private VideoShareLinkResponse mapToResponse(VideoShareLink shareLink) {
        return VideoShareLinkResponse.builder()
                .id(shareLink.getId())
                .videoId(shareLink.getVideoId())
                .createdBy(shareLink.getCreatedBy())
                .shareToken(shareLink.getShareToken())
                .description(shareLink.getDescription())
                .maxUses(shareLink.getMaxUses())
                .currentUses(shareLink.getCurrentUses())
                .passwordProtected(shareLink.isPasswordProtected())
                .passwordHash(shareLink.getPasswordHash())
                .expiresAt(shareLink.getExpiresAt())
                .createdAt(shareLink.getCreatedAt())
                .lastUsedAt(shareLink.getLastUsedAt())
                .build();
    }

    /**
     * Get embed code for a video share link (Feature 14: Share/Embed/QR System)
     */
    @GetMapping("/{id}/embed")
    public ResponseEntity<ApiResponse<String>> getEmbedCode(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "560") int width,
            @RequestParam(defaultValue = "315") int height) {
        VideoShareLink shareLink = videoShareLinkService.getVideoShareLinkById(id);
        String embedCode = videoShareLinkService.generateEmbedCode(shareLink, width, height);
        return ResponseEntity.ok(ApiResponse.success(embedCode));
    }

    /**
     * Get QR code for a video share link (Feature 14: Share/Embed/QR System)
     * Returns base64-encoded PNG image data.
     */
    @GetMapping("/{id}/qr-code")
    public ResponseEntity<ApiResponse<String>> getQrCode(
            @PathVariable UUID id) {
        VideoShareLink shareLink = videoShareLinkService.getVideoShareLinkById(id);
        String qrCodeData = videoShareLinkService.generateQrCode(shareLink);
        return ResponseEntity.ok(ApiResponse.success(qrCodeData));
    }
}
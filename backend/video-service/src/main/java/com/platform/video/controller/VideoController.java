package com.platform.video.controller;

import com.platform.common.dto.PageResponse;
import com.platform.video.dto.CreateVideoRequest;
import com.platform.video.dto.PublishRequest;
import com.platform.video.dto.UpdateVideoRequest;
import com.platform.video.dto.VideoDetailResponse;
import com.platform.video.dto.VideoResponse;
import com.platform.video.projection.VideoListProjection;
import com.platform.video.service.VideoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping
    public ResponseEntity<VideoResponse> createVideo(@Valid @RequestBody CreateVideoRequest request) {
        VideoResponse response = videoService.createVideo(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDetailResponse> getVideo(@PathVariable UUID id) {
        VideoDetailResponse response = videoService.getVideoDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<VideoDetailResponse> getVideoBySlug(@PathVariable String slug) {
        VideoDetailResponse response = videoService.getVideoBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VideoResponse> updateVideo(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVideoRequest request) {
        VideoResponse response = videoService.updateVideo(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<VideoResponse> publishVideo(
            @PathVariable UUID id,
            @Valid @RequestBody PublishRequest request) {
        VideoResponse response = videoService.publishVideo(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<VideoResponse> unpublishVideo(@PathVariable UUID id) {
        VideoResponse response = videoService.unpublishVideo(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<VideoListProjection>> getMyVideos(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(videoService.getMyVideos(page, size));
    }

    @GetMapping("/channel/{channelId}")
    public ResponseEntity<PageResponse<VideoListProjection>> getChannelVideos(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(videoService.getChannelVideos(channelId, page, size));
    }

    @GetMapping
    public ResponseEntity<PageResponse<VideoListProjection>> getPublishedVideos(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(videoService.getPublishedVideos(page, size));
    }

    @GetMapping("/{id}/hls")
    public ResponseEntity<String> getHlsManifestUrl(@PathVariable UUID id) {
        String url = videoService.generatePresignedHlsUrl(id);
        return ResponseEntity.ok(url);
    }
}
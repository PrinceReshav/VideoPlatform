package com.platform.video.controller;

import com.platform.common.dto.PageResponse;
import com.platform.common.entity.Video;
import com.platform.common.entity.VideoFileInfo;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.video.dto.BulkVideoOperationRequest;
import com.platform.video.dto.BulkVideoOperationResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final CurrentUserAccessor currentUserAccessor;

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

    /**
     * Perform bulk operations on videos (Feature 7: Video Manager)
     */
    @PostMapping("/bulk")
    public ResponseEntity<BulkVideoOperationResponse> bulkVideoOperation(
            @Valid @RequestBody BulkVideoOperationRequest request) {
        BulkVideoOperationResponse response = videoService.bulkVideoOperation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get deleted videos for the current user (Feature 7: Video Manager)
     */
    @GetMapping("/trash")
    public ResponseEntity<List<VideoDetailResponse>> getDeletedVideos() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        List<Video> deletedVideos = videoService.getDeletedVideos(userId);
        List<VideoDetailResponse> responses = deletedVideos.stream()
                .map(videoService::mapToVideoDetailResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get deleted videos with pagination for the current user (Feature 7: Video Manager)
     */
    @GetMapping("/trash/paginated")
    public ResponseEntity<PageResponse<VideoDetailResponse>> getDeletedVideosPaginated(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Page<Video> deletedVideosPage = videoService.getDeletedVideos(userId, page, size);

        List<VideoDetailResponse> videoDetailResponses = deletedVideosPage.getContent().stream()
                .map(videoService::mapToVideoDetailResponse)
                .collect(Collectors.toList());

        PageResponse<VideoDetailResponse> response = PageResponse.of(
                videoDetailResponses,
                page,
                size,
                deletedVideosPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get captions for a video (Feature 8: Captions & Auto-transcription)
     */
    @GetMapping("/{videoId}/captions")
    public ResponseEntity<List<VideoFileInfo>> getVideoCaptions(@PathVariable UUID videoId) {
        List<VideoFileInfo> captions = videoService.getVideoCaptions(videoId);
        return ResponseEntity.ok(captions);
    }

    /**
     * Add captions to a video (Feature 8: Captions & Auto-transcription)
     */
    @PostMapping("/{videoId}/captions")
    public ResponseEntity<Void> addVideoCaptions(
            @PathVariable UUID videoId,
            @RequestBody List<VideoFileInfo> captionFiles) {
        videoService.addVideoCaptions(videoId, captionFiles);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete captions from a video (Feature 8: Captions & Auto-transcription)
     */
    @DeleteMapping("/{videoId}/captions")
    public ResponseEntity<Void> deleteVideoCaptions(@PathVariable UUID videoId) {
        videoService.deleteVideoCaptions(videoId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get dubbed audio for a video (Feature 9: Multi-language Subtitles/Dubbing)
     */
    @GetMapping("/{videoId}/dubbed-audio")
    public ResponseEntity<List<VideoFileInfo>> getVideoDubbedAudio(@PathVariable UUID videoId) {
        List<VideoFileInfo> dubbedAudio = videoService.getVideoDubbedAudio(videoId);
        return ResponseEntity.ok(dubbedAudio);
    }

    /**
     * Add dubbed audio to a video (Feature 9: Multi-language Subtitles/Dubbing)
     */
    @PostMapping("/{videoId}/dubbed-audio")
    public ResponseEntity<Void> addVideoDubbedAudio(
            @PathVariable UUID videoId,
            @RequestBody List<VideoFileInfo> dubbedAudioFiles) {
        videoService.addVideoDubbedAudio(videoId, dubbedAudioFiles);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete dubbed audio from a video (Feature 9: Multi-language Subtitles/Dubbing)
     */
    @DeleteMapping("/{videoId}/dubbed-audio")
    public ResponseEntity<Void> deleteVideoDubbedAudio(@PathVariable UUID videoId) {
        videoService.deleteVideoDubbedAudio(videoId);
        return ResponseEntity.ok().build();
    }
}
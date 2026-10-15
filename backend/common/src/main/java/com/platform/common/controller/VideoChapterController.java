package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.dto.VideoChapterRequest;
import com.platform.common.dto.VideoChapterResponse;
import com.platform.common.entity.VideoChapter;
import com.platform.common.service.VideoChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for video chapter management (Feature 12: Video Chapters).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/video-chapters")
@RequiredArgsConstructor
public class VideoChapterController {

    private final VideoChapterService videoChapterService;

    /**
     * Create a new video chapter.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VideoChapterResponse>> createVideoChapter(
            @Valid @RequestBody VideoChapterRequest request) {

        VideoChapter chapter = VideoChapter.builder()
                .title(request.getTitle())
                .startTimeSeconds(request.getStartTimeSeconds())
                .endTimeSeconds(request.getEndTimeSeconds())
                .thumbnailUrl(request.getThumbnailUrl())
                .description(request.getDescription())
                .position(request.getPosition())
                .build();

        VideoChapter savedChapter =
                videoChapterService.createVideoChapter(
                        request.getVideoId(),
                        chapter
                );

        return ResponseEntity.ok(
                ApiResponse.success(mapToResponse(savedChapter))
        );
    }

    /**
     * Get a video chapter by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoChapterResponse>> getVideoChapterById(
            @PathVariable UUID id) {

        VideoChapter chapter =
                videoChapterService.getVideoChapterById(id);

        return ResponseEntity.ok(
                ApiResponse.success(mapToResponse(chapter))
        );
    }

    /**
     * Get all video chapters for a video.
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<VideoChapterResponse>>> getVideoChaptersByVideoId(
            @PathVariable UUID videoId) {

        List<VideoChapter> chapters =
                videoChapterService.getVideoChaptersByVideoId(videoId);

        List<VideoChapterResponse> responses = chapters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(responses)
        );
    }

    /**
     * Get video chapters by creation timestamp range.
     */
    @GetMapping("/timestamp-range")
    public ResponseEntity<ApiResponse<List<VideoChapterResponse>>> getVideoChaptersByTimestampRange(
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {

        List<VideoChapter> chapters =
                videoChapterService.getVideoChaptersByTimestampRange(
                        startTime,
                        endTime
                );

        List<VideoChapterResponse> responses = chapters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(responses)
        );
    }

    /**
     * Update a video chapter.
     *
     * The videoId from the request is deliberately not copied into the
     * entity because VideoChapter stores the relationship as Video video.
     * The existing chapter's video relationship remains unchanged.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoChapterResponse>> updateVideoChapter(
            @PathVariable UUID id,
            @Valid @RequestBody VideoChapterRequest request) {

        VideoChapter chapterDetails = VideoChapter.builder()
                .title(request.getTitle())
                .startTimeSeconds(request.getStartTimeSeconds())
                .endTimeSeconds(request.getEndTimeSeconds())
                .thumbnailUrl(request.getThumbnailUrl())
                .description(request.getDescription())
                .position(request.getPosition())
                .build();

        VideoChapter updatedChapter =
                videoChapterService.updateVideoChapter(
                        id,
                        chapterDetails
                );

        return ResponseEntity.ok(
                ApiResponse.success(mapToResponse(updatedChapter))
        );
    }

    /**
     * Delete a video chapter.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideoChapter(
            @PathVariable UUID id) {

        videoChapterService.deleteVideoChapter(id);

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    /**
     * Reorder video chapters for a video.
     */
    @PutMapping("/video/{videoId}/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderVideoChapters(
            @PathVariable UUID videoId,
            @RequestParam List<UUID> chapterIdsInOrder) {

        videoChapterService.reorderVideoChapters(
                videoId,
                chapterIdsInOrder
        );

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    /**
     * Convert entity to response DTO.
     */
    private VideoChapterResponse mapToResponse(VideoChapter chapter) {

        UUID videoId = null;

        if (chapter.getVideo() != null) {
            videoId = chapter.getVideo().getId();
        }

        return VideoChapterResponse.builder()
                .id(chapter.getId())
                .videoId(videoId)
                .title(chapter.getTitle())
                .startTimeSeconds(chapter.getStartTimeSeconds())
                .endTimeSeconds(chapter.getEndTimeSeconds())
                .thumbnailUrl(chapter.getThumbnailUrl())
                .description(chapter.getDescription())
                .position(chapter.getPosition())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}
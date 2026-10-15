package com.platform.video.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.video.dto.VideoVersionDTO;
import com.platform.video.service.VideoVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller for video versioning (Feature 21: Video Versioning)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/video-versions")
@RequiredArgsConstructor
public class VideoVersionController {

    private final VideoVersionService videoVersionService;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Create a new version of a video
     */
    @PostMapping("/videos/{videoId}")
    public ResponseEntity<ApiResponse<VideoVersionDTO>> createVideoVersion(
            @PathVariable UUID videoId,
            @Valid @RequestBody VideoVersionCreateRequest request) {

        UUID userId = currentUserAccessor.getCurrentUserId();

        var version = videoVersionService.createVideoVersion(videoId, request.getChangeSummary(), userId);
        var responseDTO = videoVersionService.mapToDTO(version);

        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    /**
     * Get a video version by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoVersionDTO>> getVideoVersionById(
            @PathVariable UUID id) {

        var version = videoVersionService.getVideoVersionById(id);
        var responseDTO = videoVersionService.mapToDTO(version);

        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    /**
     * Get all versions of a video
     */
    @GetMapping("/videos/{videoId}")
    public ResponseEntity<ApiResponse<List<VideoVersionDTO>>> getVideoVersions(
            @PathVariable UUID videoId) {

        var versions = videoVersionService.getVideoVersions(videoId);
        var responseDTOs = videoVersionService.mapToDTOList(versions);

        return ResponseEntity.ok(ApiResponse.success(responseDTOs));
    }

    /**
     * Get the current version of a video
     */
    @GetMapping("/videos/{videoId}/current")
    public ResponseEntity<ApiResponse<VideoVersionDTO>> getCurrentVideoVersion(
            @PathVariable UUID videoId) {

        var version = videoVersionService.getCurrentVideoVersion(videoId);
        var responseDTO = videoVersionService.mapToDTO(version);

        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    /**
     * Get video versions with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VideoVersionDTO>>> getVideoVersionsByCriteria(
            @RequestParam(required = false) UUID videoId,
            @RequestParam(required = false) Integer versionNumber,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) UUID changedBy,
            @RequestParam(required = false) Boolean isCurrent,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @PageableDefault(size = 20) Pageable pageable) {

        var versionsPage = videoVersionService.getVideoVersionsByCriteria(
                videoId, versionNumber, title, changedBy, isCurrent, startTime, endTime, pageable);

        var dtosPage = versionsPage.map(videoVersionService::mapToDTO);

        return ResponseEntity.ok(ApiResponse.success(dtosPage));
    }

    /**
     * Request DTO for creating a video version
     */
    public static class VideoVersionCreateRequest {
        private String changeSummary;

        public String getChangeSummary() {
            return changeSummary;
        }

        public void setChangeSummary(String changeSummary) {
            this.changeSummary = changeSummary;
        }
    }
}
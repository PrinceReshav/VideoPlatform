package com.platform.engagement.controller;

import com.platform.engagement.dto.SavedVideoRequest;
import com.platform.engagement.dto.SavedVideoResponse;
import com.platform.engagement.service.SavedVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for saved videos (Feature 24: Watch Later)
 */
@RestController
@RequestMapping("/api/v1/saved-videos")
@RequiredArgsConstructor
public class SavedVideoController {

    private final SavedVideoService savedVideoService;

    /**
     * Save a video for later
     */
    @PostMapping
    public ResponseEntity<SavedVideoResponse> saveVideoForLater(
            @Valid @RequestBody SavedVideoRequest request) {
        SavedVideoResponse response = savedVideoService.saveVideoForLater(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a video from saved for later
     */
    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> removeVideoFromSaved(
            @PathVariable UUID videoId) {
        savedVideoService.removeVideoFromSaved(videoId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get saved videos for the current user
     */
    @GetMapping
    public ResponseEntity<List<SavedVideoResponse>> getSavedVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<SavedVideoResponse> savedVideos = savedVideoService.getSavedVideos(page, size);
        return ResponseEntity.ok(savedVideos);
    }

    /**
     * Check if a video is saved by the current user
     */
    @GetMapping("/{videoId}/check")
    public ResponseEntity<Boolean> isVideoSaved(@PathVariable UUID videoId) {
        boolean isSaved = savedVideoService.isVideoSaved(videoId);
        return ResponseEntity.ok(isSaved);
    }
}
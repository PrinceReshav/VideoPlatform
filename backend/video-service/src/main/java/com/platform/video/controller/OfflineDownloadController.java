package com.platform.video.controller;

import com.platform.video.dto.OfflineDownloadRequest;
import com.platform.video.dto.OfflineDownloadResponse;
import com.platform.video.service.OfflineDownloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for offline downloads (Feature 34: Offline Downloads with DRM)
 */
@RestController
@RequestMapping("/api/v1/offline-downloads")
@RequiredArgsConstructor
public class OfflineDownloadController {

    private final OfflineDownloadService offlineDownloadService;

    /**
     * Create a new offline download request
     */
    @PostMapping
    public ResponseEntity<OfflineDownloadResponse> createOfflineDownload(
            @Valid @RequestBody OfflineDownloadRequest request) {
        OfflineDownloadResponse response = offlineDownloadService.createOfflineDownload(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get an offline download by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OfflineDownloadResponse> getOfflineDownload(@PathVariable UUID id) {
        OfflineDownloadResponse response = offlineDownloadService.getOfflineDownload(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get offline downloads by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OfflineDownloadResponse>> getOfflineDownloadsByUser(@PathVariable UUID userId) {
        List<OfflineDownloadResponse> responses = offlineDownloadService.getOfflineDownloadsByUser(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Mark an offline download as played
     */
    @PostMapping("/{id}/play")
    public ResponseEntity<OfflineDownloadResponse> markAsPlayed(@PathVariable UUID id) {
        OfflineDownloadResponse response = offlineDownloadService.markAsPlayed(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an offline download
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOfflineDownload(@PathVariable UUID id) {
        offlineDownloadService.deleteOfflineDownload(id);
        return ResponseEntity.noContent().build();
    }
}
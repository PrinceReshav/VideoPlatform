package com.platform.video.controller;

import com.platform.video.dto.LiveStreamRequest;
import com.platform.video.dto.LiveStreamResponse;
import com.platform.video.service.LiveStreamService;
import com.platform.video.service.LiveStreamVodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for live streams (Feature 31: Live Streaming Platform)
 */
@RestController
@RequestMapping("/api/v1/live-streams")
@RequiredArgsConstructor
public class LiveStreamController {

    private final LiveStreamService liveStreamService;
    private final LiveStreamVodService liveStreamVodService;

    /**
     * Create a new live stream
     */
    @PostMapping
    public ResponseEntity<LiveStreamResponse> createLiveStream(
            @Valid @RequestBody LiveStreamRequest request) {
        LiveStreamResponse response = liveStreamService.createLiveStream(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a live stream by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LiveStreamResponse> getLiveStream(@PathVariable UUID id) {
        LiveStreamResponse response = liveStreamService.getLiveStream(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a live stream
     */
    @PutMapping("/{id}")
    public ResponseEntity<LiveStreamResponse> updateLiveStream(
            @PathVariable UUID id,
            @Valid @RequestBody LiveStreamRequest request) {
        LiveStreamResponse response = liveStreamService.updateLiveStream(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a live stream
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLiveStream(@PathVariable UUID id) {
        liveStreamService.deleteLiveStream(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Start a live stream
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<LiveStreamResponse> startLiveStream(@PathVariable UUID id) {
        LiveStreamResponse response = liveStreamService.startLiveStream(id);
        return ResponseEntity.ok(response);
    }

    /**
     * End a live stream
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<LiveStreamResponse> endLiveStream(@PathVariable UUID id) {
        LiveStreamResponse response = liveStreamService.endLiveStream(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get live streams by creator ID
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<LiveStreamResponse>> getLiveStreamsByCreator(@PathVariable UUID creatorId) {
        List<LiveStreamResponse> responses = liveStreamService.getLiveStreamsByCreator(creatorId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get online live streams (for viewers to watch)
     */
    @GetMapping("/online")
    public ResponseEntity<List<LiveStreamResponse>> getOnlineLiveStreams() {
        List<LiveStreamResponse> responses = liveStreamService.getOnlineLiveStreams();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get live stream by stream key (used by streaming infrastructure)
     */
    @GetMapping("/stream-key/{streamKey}")
    public ResponseEntity<LiveStreamResponse> getLiveStreamByStreamKey(@PathVariable String streamKey) {
        LiveStreamResponse response = liveStreamService.getLiveStreamByStreamKey(streamKey);
        return ResponseEntity.ok(response);
    }

    /**
     * Manually convert a live stream to VOD (typically used internally)
     */
    @PostMapping("/{id}/convert-to-vod")
    public ResponseEntity<UUID> convertLiveStreamToVod(@PathVariable UUID id) {
        UUID vodVideoId = liveStreamVodService.convertLiveStreamToVod(id);
        return ResponseEntity.ok(vodVideoId);
    }
}
package com.platform.video.controller;

import com.platform.video.dto.LiveChatMessageRequest;
import com.platform.video.dto.LiveChatMessageResponse;
import com.platform.video.service.LiveChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller for live chat messages (Feature 31: Live Streaming Platform)
 */
@RestController
@RequestMapping("/api/v1/live-chat")
@RequiredArgsConstructor
public class LiveChatMessageController {

    private final LiveChatMessageService liveChatMessageService;

    /**
     * Send a chat message in a live stream
     */
    @PostMapping("/streams/{liveStreamId}/messages")
    public ResponseEntity<LiveChatMessageResponse> sendChatMessage(
            @PathVariable UUID liveStreamId,
            @Valid @RequestBody LiveChatMessageRequest request) {
        LiveChatMessageResponse response = liveChatMessageService.sendChatMessage(liveStreamId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get chat messages for a live stream
     */
    @GetMapping("/streams/{liveStreamId}/messages")
    public ResponseEntity<List<LiveChatMessageResponse>> getChatMessages(
            @PathVariable UUID liveStreamId,
            @RequestParam(required = false) Integer limit) {
        List<LiveChatMessageResponse> responses = liveChatMessageService.getChatMessages(liveStreamId, limit);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get new chat messages for a live stream since a given time
     */
    @GetMapping("/streams/{liveStreamId}/messages/new")
    public ResponseEntity<List<LiveChatMessageResponse>> getNewChatMessages(
            @PathVariable UUID liveStreamId,
            @RequestParam Instant since) {
        List<LiveChatMessageResponse> responses = liveChatMessageService.getNewChatMessages(liveStreamId, since);
        return ResponseEntity.ok(responses);
    }

    /**
     * Delete a chat message
     */
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteChatMessage(@PathVariable UUID id) {
        liveChatMessageService.deleteChatMessage(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Add a system message to a live stream (typically used by internal services)
     */
    @PostMapping("/streams/{liveStreamId}/system-messages")
    public ResponseEntity<LiveChatMessageResponse> addSystemMessage(
            @PathVariable UUID liveStreamId,
            @RequestParam String message) {
        LiveChatMessageResponse response = liveChatMessageService.addSystemMessage(liveStreamId, message);
        return ResponseEntity.ok(response);
    }
}
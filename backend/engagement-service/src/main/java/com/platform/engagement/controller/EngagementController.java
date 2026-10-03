package com.platform.engagement.controller;

import com.platform.common.dto.PageResponse;
import com.platform.engagement.dto.CommentRequest;
import com.platform.engagement.dto.CommentResponse;
import com.platform.engagement.dto.LikeRequest;
import com.platform.engagement.dto.LikeResponse;
import com.platform.engagement.dto.UpdateCommentRequest;
import com.platform.engagement.dto.WatchHistoryResponse;
import com.platform.engagement.service.EngagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/engagement")
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;

    @PostMapping("/likes")
    public ResponseEntity<LikeResponse> toggleLike(@Valid @RequestBody LikeRequest request) {
        return ResponseEntity.ok(engagementService.toggleLike(request));
    }


    @GetMapping("/likes/videos")
    public ResponseEntity<PageResponse<WatchHistoryResponse>> getLikedVideos(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(engagementService.getLikedVideos(page, size));
    }

    @GetMapping("/likes/{videoId}")
    public ResponseEntity<Boolean> isLiked(@PathVariable UUID videoId) {
        return ResponseEntity.ok(engagementService.isLiked(videoId));
    }

    @GetMapping("/likes/{videoId}/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable UUID videoId) {
        return ResponseEntity.ok(engagementService.getLikeCount(videoId));
    }

    @PostMapping("/comments")
    public ResponseEntity<CommentResponse> addComment(@Valid @RequestBody CommentRequest request) {
        CommentResponse response = engagementService.addComment(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentResponse response = engagementService.updateComment(commentId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        engagementService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/comments/{videoId}")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable UUID videoId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(engagementService.getComments(videoId, page, size));
    }

    @GetMapping("/comments/{videoId}/thread")
    public ResponseEntity<List<CommentResponse>> getCommentThread(@PathVariable UUID videoId) {
        return ResponseEntity.ok(engagementService.getCommentThread(videoId));
    }

    @PostMapping("/views/{videoId}")
    public ResponseEntity<Void> recordView(
            @PathVariable UUID videoId,
            @RequestParam(required = false) Integer position,
            @RequestParam(required = false) Integer duration,
            @RequestParam(required = false) String sessionId) {
        engagementService.recordView(videoId, position, duration, sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/watch-history")
    public ResponseEntity<PageResponse<WatchHistoryResponse>> getWatchHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(engagementService.getWatchHistory(page, size));
    }

    @GetMapping("/watch-history/continue")
    public ResponseEntity<List<WatchHistoryResponse>> getContinueWatching() {
        return ResponseEntity.ok(engagementService.getContinueWatching());
    }
}
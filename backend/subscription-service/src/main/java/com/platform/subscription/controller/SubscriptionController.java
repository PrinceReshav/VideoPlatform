package com.platform.subscription.controller;

import com.platform.common.dto.PageResponse;
import com.platform.subscription.dto.FeedItemResponse;
import com.platform.subscription.dto.SubscribeRequest;
import com.platform.subscription.dto.SubscriptionResponse;
import com.platform.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(@Valid @RequestBody SubscribeRequest request) {
        SubscriptionResponse response = subscriptionService.subscribe(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> unsubscribe(@PathVariable UUID channelId) {
        subscriptionService.unsubscribe(channelId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<Boolean> isSubscribed(@PathVariable UUID channelId) {
        return ResponseEntity.ok(subscriptionService.isSubscribed(channelId));
    }

    @GetMapping("/{channelId}/count")
    public ResponseEntity<Long> getSubscriberCount(@PathVariable UUID channelId) {
        return ResponseEntity.ok(subscriptionService.getSubscriberCount(channelId));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<SubscriptionResponse>> getMySubscriptions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(page, size));
    }

    @GetMapping("/channel/{channelId}/subscribers")
    public ResponseEntity<PageResponse<SubscriptionResponse>> getChannelSubscribers(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(subscriptionService.getChannelSubscribers(channelId, page, size));
    }

    @GetMapping("/feed")
    public ResponseEntity<PageResponse<FeedItemResponse>> getSubscriptionFeed(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionFeed(page, size));
    }
}
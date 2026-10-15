package com.platform.user.controller;

import com.platform.user.dto.ChannelResponse;
import com.platform.user.dto.CreateChannelRequest;
import com.platform.user.dto.UpdateChannelRequest;
import com.platform.user.service.ChannelService;
import com.platform.common.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping("/me")
    public ResponseEntity<ChannelResponse> getMyChannel() {
        return ResponseEntity.ok(channelService.getMyChannel());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponse> getChannel(@PathVariable UUID id) {
        return ResponseEntity.ok(channelService.getChannelById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ChannelResponse> getChannelBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(channelService.getChannelBySlug(slug));
    }

    @GetMapping("/custom/{customUrl}")
    public ResponseEntity<ChannelResponse> getChannelByCustomUrl(@PathVariable String customUrl) {
        return ResponseEntity.ok(channelService.getChannelByCustomUrl(customUrl));
    }

    @GetMapping
    public ResponseEntity<List<ChannelResponse>> getActiveChannels() {
        return ResponseEntity.ok(channelService.getActiveChannels());
    }

    @GetMapping("/page")
    public ResponseEntity<PageResponse<ChannelResponse>> getChannelsPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(channelService.getChannels(page, size));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ChannelResponse>> getChannelsByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(channelService.getChannelsByCategory(categoryId));
    }

    @GetMapping("/verified")
    public ResponseEntity<List<ChannelResponse>> getVerifiedChannels() {
        return ResponseEntity.ok(channelService.getVerifiedChannels());
    }

    /** Create a new channel (Feature 15: Creator Channels/Profiles) */
    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(@Valid @RequestBody CreateChannelRequest request) {
        return ResponseEntity.ok(channelService.createChannel(request));
    }

    /** Update an existing channel (Feature 15: Creator Channels/Profiles) */
    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponse> updateChannel(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChannelRequest request) {
        return ResponseEntity.ok(channelService.updateChannel(id, request));
    }
}
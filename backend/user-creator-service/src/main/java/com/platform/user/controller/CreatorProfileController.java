package com.platform.user.controller;

import com.platform.user.dto.*;
import com.platform.user.service.CreatorProfileService;
import com.platform.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators")
@RequiredArgsConstructor
public class CreatorProfileController {

    private final CreatorProfileService creatorProfileService;

    @PostMapping
    public ResponseEntity<CreatorProfileResponse> becomeCreator(@Valid @RequestBody CreateCreatorProfileRequest request) {
        CreatorProfileResponse response = creatorProfileService.becomeCreator(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CreatorProfileResponse> getMyCreatorProfile() {
        CreatorProfileResponse response = creatorProfileService.getMyCreatorProfile();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreatorProfileResponse> getCreatorProfile(@PathVariable UUID id) {
        CreatorProfileResponse response = creatorProfileService.getCreatorProfileById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/channel/{channelSlug}")
    public ResponseEntity<CreatorProfileResponse> getCreatorProfileByChannelSlug(@PathVariable String channelSlug) {
        CreatorProfileResponse response = creatorProfileService.getCreatorProfileByChannelSlug(channelSlug);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<CreatorProfileResponse> updateCreatorProfile(@Valid @RequestBody UpdateCreatorProfileRequest request) {
        CreatorProfileResponse response = creatorProfileService.updateCreatorProfile(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCreatorProfile() {
        creatorProfileService.deleteCreatorProfile();
        return ResponseEntity.ok().build();
    }
}
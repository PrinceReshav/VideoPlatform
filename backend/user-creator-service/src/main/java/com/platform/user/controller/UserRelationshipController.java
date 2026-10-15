package com.platform.user.controller;

import com.platform.user.dto.UserRelationshipRequest;
import com.platform.user.dto.UserRelationshipResponse;
import com.platform.user.service.UserRelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for user relationships (Feature 26: Block/Mute/Restrict)
 */
@RestController
@RequestMapping("/api/v1/user-relationships")
@RequiredArgsConstructor
public class UserRelationshipController {

    private final UserRelationshipService userRelationshipService;

    /**
     * Create a user relationship (block, mute, restrict)
     */
    @PostMapping
    public ResponseEntity<UserRelationshipResponse> createUserRelationship(
            @Valid @RequestBody UserRelationshipRequest request) {
        UserRelationshipResponse response = userRelationshipService.createUserRelationship(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a user relationship (unblock, unmute, unrestrict)
     */
    @DeleteMapping("/{targetUserId}/{relationshipType}")
    public ResponseEntity<Void> removeUserRelationship(
            @PathVariable UUID targetUserId,
            @PathVariable String relationshipType) {
        userRelationshipService.removeUserRelationship(targetUserId, relationshipType);
        return ResponseEntity.ok().build();
    }

    /**
     * Get relationships where the current user is the initiator (blocker, muter, etc.)
     */
    @GetMapping("/initiated")
    public ResponseEntity<List<UserRelationshipResponse>> getInitiatedRelationships(
            @RequestParam(required = false) String relationshipType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<UserRelationshipResponse> relationships = userRelationshipService.getInitiatedRelationships(relationshipType, page, size);
        return ResponseEntity.ok(relationships);
    }

    /**
     * Get relationships where the current user is the target (blocked, muted, etc.)
     */
    @GetMapping("/received")
    public ResponseEntity<List<UserRelationshipResponse>> getReceivedRelationships(
            @RequestParam(required = false) String relationshipType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<UserRelationshipResponse> relationships = userRelationshipService.getReceivedRelationships(relationshipType, page, size);
        return ResponseEntity.ok(relationships);
    }

    /**
     * Check if a relationship exists between the current user and target user
     */
    @GetMapping("/check/{targetUserId}/{relationshipType}")
    public ResponseEntity<Boolean> hasRelationship(
            @PathVariable UUID targetUserId,
            @PathVariable String relationshipType) {
        boolean hasRelationship = userRelationshipService.hasRelationship(targetUserId, relationshipType);
        return ResponseEntity.ok(hasRelationship);
    }
}
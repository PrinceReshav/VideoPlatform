package com.platform.playlist.controller;

import com.platform.playlist.entity.PlaylistCollaborator;

import com.platform.common.dto.PageResponse;
import com.platform.playlist.dto.AddVideoToPlaylistRequest;
import com.platform.playlist.dto.CreatePlaylistRequest;
import com.platform.playlist.dto.PlaylistResponse;
import com.platform.playlist.dto.ReorderPlaylistRequest;
import com.platform.playlist.dto.UpdatePlaylistRequest;
import com.platform.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping
    public ResponseEntity<PlaylistResponse> createPlaylist(@Valid @RequestBody CreatePlaylistRequest request) {
        PlaylistResponse response = playlistService.createPlaylist(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable UUID id) {
        PlaylistResponse response = playlistService.getPlaylist(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlaylistResponse> updatePlaylist(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlaylistRequest request) {
        PlaylistResponse response = playlistService.updatePlaylist(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable UUID id) {
        playlistService.deletePlaylist(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{playlistId}/videos")
    public ResponseEntity<Void> addVideoToPlaylist(
            @PathVariable UUID playlistId,
            @Valid @RequestBody AddVideoToPlaylistRequest request) {
        playlistService.addVideoToPlaylist(playlistId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{playlistId}/videos/{videoId}")
    public ResponseEntity<Void> removeVideoFromPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable UUID videoId) {
        playlistService.removeVideoFromPlaylist(playlistId, videoId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{playlistId}/reorder")
    public ResponseEntity<Void> reorderPlaylist(
            @PathVariable UUID playlistId,
            @Valid @RequestBody ReorderPlaylistRequest request) {
        playlistService.reorderPlaylist(playlistId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<PlaylistResponse>> getMyPlaylists(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(playlistService.getMyPlaylists(page, size));
    }

    @GetMapping("/public")
    public ResponseEntity<PageResponse<PlaylistResponse>> getPublicPlaylists(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(playlistService.getPublicPlaylists(page, size));
    }

    @GetMapping("/{playlistId}/items")
    public ResponseEntity<List<com.platform.playlist.entity.PlaylistItem>> getPlaylistItems(@PathVariable UUID playlistId) {
        return ResponseEntity.ok(playlistService.getPlaylistItems(playlistId));
    }

    /**
     * Collaborator management endpoints (Feature 25: Advanced Playlists)
     */
    @PostMapping("/{playlistId}/collaborators")
    public ResponseEntity<Void> addCollaborator(
            @PathVariable UUID playlistId,
            @RequestParam UUID userId,
            @RequestParam String permissionLevel) {
        PlaylistCollaborator.PermissionLevel level = PlaylistCollaborator.PermissionLevel.valueOf(permissionLevel);
        playlistService.addCollaborator(playlistId, userId, level);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{playlistId}/collaborators/{userId}")
    public ResponseEntity<Void> removeCollaborator(
            @PathVariable UUID playlistId,
            @PathVariable UUID userId) {
        playlistService.removeCollaborator(playlistId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{playlistId}/collaborators")
    public ResponseEntity<List<UUID>> getCollaborators(@PathVariable UUID playlistId) {
        List<UUID> collaborators = playlistService.getCollaborators(playlistId);
        return ResponseEntity.ok(collaborators);
    }

    @PutMapping("/{playlistId}/collaborators/{userId}/permission")
    public ResponseEntity<Void> updateCollaboratorPermission(
            @PathVariable UUID playlistId,
            @PathVariable UUID userId,
            @RequestParam String permissionLevel) {
        PlaylistCollaborator.PermissionLevel level = PlaylistCollaborator.PermissionLevel.valueOf(permissionLevel);
        playlistService.updateCollaboratorPermission(playlistId, userId, level);
        return ResponseEntity.ok().build();
    }
}
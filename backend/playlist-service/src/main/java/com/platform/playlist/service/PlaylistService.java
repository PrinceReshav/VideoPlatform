package com.platform.playlist.service;

import com.platform.playlist.entity.PlaylistCollaborator;
import com.platform.playlist.repository.PlaylistCollaboratorRepository;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import com.platform.playlist.dto.AddVideoToPlaylistRequest;
import com.platform.playlist.dto.CreatePlaylistRequest;
import com.platform.playlist.dto.PlaylistResponse;
import com.platform.playlist.dto.ReorderPlaylistRequest;
import com.platform.playlist.dto.UpdatePlaylistRequest;
import com.platform.playlist.entity.Playlist;
import com.platform.playlist.entity.PlaylistItem;
import com.platform.playlist.repository.PlaylistItemRepository;
import com.platform.playlist.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final PlaylistCollaboratorRepository playlistCollaboratorRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final VideoRepository videoRepository;

    @Transactional
    public PlaylistResponse createPlaylist(CreatePlaylistRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        Playlist playlist = new Playlist();
        playlist.setUserId(userId);
        playlist.setName(request.getName());
        playlist.setDescription(request.getDescription());
        playlist.setVisibility(request.getVisibility());
        playlist.setCollaborative(request.isCollaborative());
        playlist.setAutoAddEnabled(request.isAutoAddEnabled());
        playlist.setAutoAddRules(request.getAutoAddRules());
        playlist.setLastModifiedAt(Instant.now());
        playlist = playlistRepository.save(playlist);

        log.info("Created playlist: {} ({})", playlist.getName(), playlist.getId());
        return mapToPlaylistResponse(playlist);
    }

    @Transactional(readOnly = true)
    public PlaylistResponse getPlaylist(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        checkPlaylistAccess(playlist);
        return mapToPlaylistResponse(playlist);
    }

    @Transactional
    public PlaylistResponse updatePlaylist(UUID playlistId, UpdatePlaylistRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        if (!playlist.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to update this playlist");
        }

        if (request.getName() != null) playlist.setName(request.getName());
        if (request.getDescription() != null) playlist.setDescription(request.getDescription());
        if (request.getVisibility() != null) playlist.setVisibility(request.getVisibility());
        if (request.getIsCollaborative() != null) playlist.setCollaborative(request.getIsCollaborative());
        if (request.getAutoAddEnabled() != null) playlist.setAutoAddEnabled(request.getAutoAddEnabled());
        if (request.getAutoAddRules() != null) playlist.setAutoAddRules(request.getAutoAddRules());
        playlist.setLastModifiedAt(Instant.now());

        playlist = playlistRepository.save(playlist);
        log.info("Updated playlist: {}", playlistId);
        return mapToPlaylistResponse(playlist);
    }

    @Transactional
    public void deletePlaylist(UUID playlistId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        if (!playlist.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to delete this playlist");
        }

        // Delete items first
        playlistItemRepository.deleteByPlaylistId(playlistId);

        // Soft delete playlist
        playlist.markDeleted(userId);
        playlistRepository.save(playlist);

        log.info("Deleted playlist: {}", playlistId);
    }

    @Transactional
    public void addVideoToPlaylist(UUID playlistId, AddVideoToPlaylistRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        if (!playlist.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to modify this playlist");
        }

        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));
        if (video.isDeleted()) {
            throw new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found");
        }

        // Check if already in playlist
        if (playlistItemRepository.findByPlaylistIdAndVideoId(playlistId, request.getVideoId()).isPresent()) {
            throw new BusinessException(ApiErrorCodes.VIDEO_ALREADY_IN_PLAYLIST,
                    org.springframework.http.HttpStatus.CONFLICT, "Video already in playlist");
        }

        int position = request.getPosition() != null ? request.getPosition() : playlist.getVideoCount() + 1;

        // Shift positions if needed
        if (position <= playlist.getVideoCount()) {
            playlistItemRepository.shiftPositionsForward(playlistId, position);
        }

        PlaylistItem item = new PlaylistItem();
        item.setPlaylistId(playlistId);
        item.setVideoId(request.getVideoId());
        item.setPosition(position);
        playlistItemRepository.save(item);

        // Update playlist stats - duration unknown without video-service
        playlist.setVideoCount(playlist.getVideoCount() + 1);
        playlistRepository.save(playlist);

        log.info("Added video {} to playlist {}", request.getVideoId(), playlistId);
    }

    @Transactional
    public void removeVideoFromPlaylist(UUID playlistId, UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        if (!playlist.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to modify this playlist");
        }

        PlaylistItem item = playlistItemRepository.findByPlaylistIdAndVideoId(playlistId, videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_VIDEO_NOT_FOUND, "Video not in playlist"));

        int position = item.getPosition();
        playlistItemRepository.delete(item);

        // Shift positions backward
        playlistItemRepository.shiftPositionsBackward(playlistId, position);

        // Update playlist stats - duration unknown without video-service
        playlist.setVideoCount(Math.max(0, playlist.getVideoCount() - 1));
        playlistRepository.save(playlist);

        log.info("Removed video {} from playlist {}", videoId, playlistId);
    }

    @Transactional
    public void reorderPlaylist(UUID playlistId, ReorderPlaylistRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        if (!playlist.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to modify this playlist");
        }

        // Verify all videos belong to this playlist
        List<PlaylistItem> existingItems = playlistItemRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        Set<UUID> existingVideoIds = existingItems.stream()
                .map(PlaylistItem::getVideoId)
                .collect(Collectors.toSet());

        if (!existingVideoIds.containsAll(request.getVideoIds()) || existingVideoIds.size() != request.getVideoIds().size()) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Video list mismatch");
        }

        // Delete all items and recreate in new order
        playlistItemRepository.deleteByPlaylistId(playlistId);

        int position = 1;
        for (UUID videoId : request.getVideoIds()) {
            PlaylistItem item = new PlaylistItem();
            item.setPlaylistId(playlistId);
            item.setVideoId(videoId);
            item.setPosition(position++);
            playlistItemRepository.save(item);
        }

        log.info("Reordered playlist: {}", playlistId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistResponse> getMyPlaylists(int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Playlist> playlists = playlistRepository.findByUserId(userId, pageable);

        return PageResponse.of(
                playlists.getContent().stream().map(this::mapToPlaylistResponse).toList(),
                page, size, playlists.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistResponse> getPublicPlaylists(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Playlist> playlists = playlistRepository.findByVisibility(Playlist.Visibility.PUBLIC, pageable);

        return PageResponse.of(
                playlists.getContent().stream().map(this::mapToPlaylistResponse).toList(),
                page, size, playlists.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public List<PlaylistItem> getPlaylistItems(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        checkPlaylistAccess(playlist);
        return playlistItemRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
    }

    private void checkPlaylistAccess(Playlist playlist) {
        UUID currentUserId = currentUserAccessor.getCurrentUserId();

        if (playlist.getVisibility() == Playlist.Visibility.PUBLIC) {
            return;
        }

        if (playlist.getUserId().equals(currentUserId) || currentUserAccessor.isAdmin()) {
            return;
        }

        if (playlist.getVisibility() == Playlist.Visibility.UNLISTED) {
            return;
        }

        throw new BusinessException(ApiErrorCodes.PLAYLIST_NOT_FOUND,
                org.springframework.http.HttpStatus.FORBIDDEN, "Playlist not accessible");
    }

    private PlaylistResponse mapToPlaylistResponse(Playlist playlist) {
        return PlaylistResponse.builder()
                .id(playlist.getId())
                .userId(playlist.getUserId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .visibility(playlist.getVisibility())
                .isCollaborative(playlist.isCollaborative())
                .autoAddEnabled(playlist.isAutoAddEnabled())
                .autoAddRules(playlist.getAutoAddRules())
                .lastModifiedAt(playlist.getLastModifiedAt())
                .followerCount(playlist.getFollowerCount())
                .videoCount(playlist.getVideoCount())
                .totalDurationSeconds(playlist.getTotalDurationSeconds())
                .thumbnailUrl(playlist.getThumbnailUrl())
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .build();
    }

    /**
     * Add a collaborator to a playlist
     */
    @Transactional
    public void addCollaborator(UUID playlistId, UUID userId, PlaylistCollaborator.PermissionLevel permissionLevel) {
        UUID currentUserId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        // Check if current user owns the playlist or is an admin
        if (!playlist.getUserId().equals(currentUserId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to modify this playlist");
        }

        // Check if already a collaborator
        if (playlistCollaboratorRepository.existsByPlaylistIdAndUserId(playlistId, userId)) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                    org.springframework.http.HttpStatus.CONFLICT, "User is already a collaborator on this playlist");
        }

        PlaylistCollaborator collaborator = new PlaylistCollaborator();
        collaborator.setPlaylistId(playlistId);
        collaborator.setUserId(userId);
        collaborator.setPermissionLevel(permissionLevel);
        collaborator.setCreatedAt(Instant.now());
        collaborator.setUpdatedAt(Instant.now());
        playlistCollaboratorRepository.save(collaborator);

        // Update the playlist's last modified time
        playlist.setLastModifiedAt(Instant.now());
        playlistRepository.save(playlist);

        log.info("Added collaborator {} to playlist {} with permission {}", userId, playlistId, permissionLevel);
    }

    /**
     * Remove a collaborator from a playlist
     */
    @Transactional
    public void removeCollaborator(UUID playlistId, UUID userId) {
        UUID currentUserId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        // Check if current user owns the playlist or is an admin
        if (!playlist.getUserId().equals(currentUserId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to modify this playlist");
        }

        // Prevent removing the owner
        if (playlist.getUserId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot remove the playlist owner");
        }

        PlaylistCollaborator collaborator = playlistCollaboratorRepository.findByPlaylistIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Collaborator not found"));

        playlistCollaboratorRepository.delete(collaborator);

        // Update the playlist's last modified time
        playlist.setLastModifiedAt(Instant.now());
        playlistRepository.save(playlist);

        log.info("Removed collaborator {} from playlist {}", userId, playlistId);
    }

    /**
     * Get collaborators for a playlist
     */
    @Transactional(readOnly = true)
    public List<UUID> getCollaborators(UUID playlistId) {
        UUID currentUserId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        // Check if current user has access to the playlist
        if (!playlist.getUserId().equals(currentUserId) && !currentUserAccessor.isAdmin()) {
            // Check if user is a collaborator
            boolean isCollaborator = playlistCollaboratorRepository.existsByPlaylistIdAndUserId(playlistId, currentUserId);
            if (!isCollaborator) {
                throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                        org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to view collaborators");
            }
        }

        List<PlaylistCollaborator> collaborators = playlistCollaboratorRepository.findByPlaylistId(playlistId);
        return collaborators.stream()
                .map(PlaylistCollaborator::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * Update collaborator permission level
     */
    @Transactional
    public void updateCollaboratorPermission(UUID playlistId, UUID userId, PlaylistCollaborator.PermissionLevel permissionLevel) {
        UUID currentUserId = currentUserAccessor.getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Playlist not found"));

        // Check if current user owns the playlist or is an admin
        if (!playlist.getUserId().equals(currentUserId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to modify this playlist");
        }

        // Prevent changing owner's permissions
        if (playlist.getUserId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot modify the playlist owner's permissions");
        }

        PlaylistCollaborator collaborator = playlistCollaboratorRepository.findByPlaylistIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.PLAYLIST_NOT_FOUND, "Collaborator not found"));

        collaborator.setPermissionLevel(permissionLevel);
        collaborator.setUpdatedAt(Instant.now());
        playlistCollaboratorRepository.save(collaborator);

        // Update the playlist's last modified time
        playlist.setLastModifiedAt(Instant.now());
        playlistRepository.save(playlist);

        log.info("Updated permission for collaborator {} on playlist {} to {}", userId, playlistId, permissionLevel);
    }
}
package com.platform.engagement.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import com.platform.engagement.dto.SavedVideoRequest;
import com.platform.engagement.dto.SavedVideoResponse;
import com.platform.engagement.entity.SavedVideo;
import com.platform.engagement.repository.SavedVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling saved videos (Feature 24: Watch Later)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavedVideoService {

    private final SavedVideoRepository savedVideoRepository;
    private final VideoRepository videoRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Save a video for later
     */
    @Transactional
    public SavedVideoResponse saveVideoForLater(SavedVideoRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        UUID videoId = request.getVideoId();

        // Verify video exists
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        // Check if already saved
        if (savedVideoRepository.findByUserIdAndVideoId(userId, videoId).isPresent()) {
            throw new BusinessException(ApiErrorCodes.VIDEO_ALREADY_IN_PLAYLIST,
                    "Video is already saved for later");
        }

        SavedVideo savedVideo = new SavedVideo();
        savedVideo.setUserId(userId);
        savedVideo.setVideoId(videoId);
        savedVideo.setSavedAt(Instant.now());

        SavedVideo saved = savedVideoRepository.save(savedVideo);
        log.info("Saved video {} for later by user {}", videoId, userId);

        return mapToResponseDTO(saved);
    }

    /**
     * Remove a video from saved for later
     */
    @Transactional
    public void removeVideoFromSaved(UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        SavedVideo savedVideo = savedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found in saved list"));

        savedVideoRepository.delete(savedVideo);
        log.info("Removed video {} from saved for later by user {}", videoId, userId);
    }

    /**
     * Get saved videos for the current user
     */
    @Transactional(readOnly = true)
    public List<SavedVideoResponse> getSavedVideos(int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        // For simplicity, we'll get all without pagination in this example
        // In a real implementation, you'd want to add pagination support
        List<SavedVideo> savedVideos = savedVideoRepository.findByUserId(userId);
        return savedVideos.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if a video is saved by the current user
     */
    @Transactional(readOnly = true)
    public boolean isVideoSaved(UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        return savedVideoRepository.findByUserIdAndVideoId(userId, videoId).isPresent();
    }

    /**
     * Convert SavedVideo entity to SavedVideoResponse DTO
     */
    private SavedVideoResponse mapToResponseDTO(SavedVideo savedVideo) {
        return SavedVideoResponse.builder()
                .id(savedVideo.getId())
                .userId(savedVideo.getUserId())
                .videoId(savedVideo.getVideoId())
                .savedAt(savedVideo.getSavedAt())
                .build();
    }

    /**
     * Convert list of SavedVideo entities to list of SavedVideoResponse DTOs
     */
    private List<SavedVideoResponse> mapToResponseDTOList(List<SavedVideo> savedVideos) {
        return savedVideos.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
}
package com.platform.video.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.video.entity.LiveStream;
import com.platform.video.repository.LiveStreamRepository;
import com.platform.video.repository.VideoRepository;
import com.platform.video.dto.LiveStreamRequest;
import com.platform.video.dto.LiveStreamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling live streams (Feature 31: Live Streaming Platform)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final VideoRepository videoRepository;
    private final CurrentUserAccessor currentUserAccessor;

    // Service for converting live streams to VOD
    private final LiveStreamVodService liveStreamVodService;

    /**
     * Create a new live stream
     */
    @Transactional
    public LiveStreamResponse createLiveStream(LiveStreamRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        // Generate a unique stream key
        String streamKey = generateStreamKey();

        LiveStream liveStream = new LiveStream();
        liveStream.setCreatorId(userId);
        liveStream.setChannelId(request.getChannelId());
        liveStream.setTitle(request.getTitle());
        liveStream.setDescription(request.getDescription());
        liveStream.setStreamKey(streamKey);
        liveStream.setStatus(LiveStream.LiveStreamStatus.OFFLINE);
        liveStream.setSaveAsVod(request.isSaveAsVod());
        liveStream.setCreatedAt(Instant.now());
        liveStream.setUpdatedAt(Instant.now());

        LiveStream savedStream = liveStreamRepository.save(liveStream);
        log.info("Created live stream {} for user {}", savedStream.getId(), userId);

        return LiveStreamResponse.fromEntity(savedStream);
    }

    /**
     * Get a live stream by ID
     */
    @Transactional(readOnly = true)
    public LiveStreamResponse getLiveStream(UUID id) {
        LiveStream liveStream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        // Check if user has permission to view this live stream
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!liveStream.getCreatorId().equals(userId)) {
            // For now, only allow creators to view their own streams
            // In a real implementation, you might allow viewers to watch online streams
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to view this live stream");
        }

        return LiveStreamResponse.fromEntity(liveStream);
    }

    /**
     * Update a live stream
     */
    @Transactional
    public LiveStreamResponse updateLiveStream(UUID id, LiveStreamRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        LiveStream liveStream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        // Check if user owns this live stream
        if (!liveStream.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to update this live stream");
        }

        // Only allow updates when stream is offline
        if (liveStream.getStatus() != LiveStream.LiveStreamStatus.OFFLINE) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Can only update offline live streams");
        }

        liveStream.setChannelId(request.getChannelId());
        liveStream.setTitle(request.getTitle());
        liveStream.setDescription(request.getDescription());
        liveStream.setSaveAsVod(request.isSaveAsVod());
        liveStream.setUpdatedAt(Instant.now());

        LiveStream updatedStream = liveStreamRepository.save(liveStream);
        log.info("Updated live stream {} for user {}", updatedStream.getId(), userId);

        return LiveStreamResponse.fromEntity(updatedStream);
    }

    /**
     * Delete a live stream
     */
    @Transactional
    public void deleteLiveStream(UUID id) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        LiveStream liveStream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        // Check if user owns this live stream
        if (!liveStream.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to delete this live stream");
        }

        // Only allow deletion when stream is offline
        if (liveStream.getStatus() != LiveStream.LiveStreamStatus.OFFLINE) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Can only delete offline live streams");
        }

        liveStreamRepository.delete(liveStream);
        log.info("Deleted live stream {} for user {}", id, userId);
    }

    /**
     * Start a live stream
     */
    @Transactional
    public LiveStreamResponse startLiveStream(UUID id) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        LiveStream liveStream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        // Check if user owns this live stream
        if (!liveStream.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to start this live stream");
        }

        // Check if stream is offline
        if (liveStream.getStatus() != LiveStream.LiveStreamStatus.OFFLINE) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Can only start offline live streams");
        }

        liveStream.setStatus(LiveStream.LiveStreamStatus.STARTING);
        liveStream.setStartedAt(Instant.now());
        liveStream.setViewerCount(0); // Reset viewer count
        liveStream.setUpdatedAt(Instant.now());

        LiveStream updatedStream = liveStreamRepository.save(liveStream);
        log.info("Started live stream {} for user {}", updatedStream.getId(), userId);

        return LiveStreamResponse.fromEntity(updatedStream);
    }

    /**
     * End a live stream
     */
    @Transactional
    public LiveStreamResponse endLiveStream(UUID id) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        LiveStream liveStream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        // Check if user owns this live stream
        if (!liveStream.getCreatorId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to end this live stream");
        }

        // Check if stream is online or starting
        if (liveStream.getStatus() != LiveStream.LiveStreamStatus.ONLINE &&
            liveStream.getStatus() != LiveStream.LiveStreamStatus.STARTING) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Can only end online or starting live streams");
        }

        liveStream.setStatus(LiveStream.LiveStreamStatus.ENDING);
        liveStream.setEndedAt(Instant.now());
        liveStream.setUpdatedAt(Instant.now());

        // If configured to save as VOD, create a video from the live stream
        if (liveStream.isSaveAsVod()) {
            // Convert the live stream to VOD
            UUID vodVideoId = liveStreamVodService.convertLiveStreamToVod(id);
            liveStream.setVodVideoId(vodVideoId);
            log.info("Live stream {} saved as VOD video {}", liveStream.getId(), vodVideoId);
        }

        liveStream.setStatus(LiveStream.LiveStreamStatus.OFFLINE);
        LiveStream updatedStream = liveStreamRepository.save(liveStream);
        log.info("Ended live stream {} for user {}", updatedStream.getId(), userId);

        return LiveStreamResponse.fromEntity(updatedStream);
    }

    /**
     * Get live streams by creator ID
     */
    @Transactional(readOnly = true)
    public List<LiveStreamResponse> getLiveStreamsByCreator(UUID creatorId) {
        // Check if user is requesting their own streams
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!userId.equals(creatorId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You can only view your own live streams");
        }

        List<LiveStream> streams = liveStreamRepository.findByCreatorId(creatorId);
        return streams.stream()
                .map(LiveStreamResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get online live streams (for viewers to watch)
     */
    @Transactional(readOnly = true)
    public List<LiveStreamResponse> getOnlineLiveStreams() {
        List<LiveStream> streams = liveStreamRepository.findOnlineStreams(
                LiveStream.LiveStreamStatus.ONLINE
        );
        return streams.stream()
                .map(LiveStreamResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get live stream by stream key (used by streaming infrastructure)
     */

    @Transactional(readOnly = true)
    public LiveStreamResponse getLiveStreamByStreamKey(String streamKey) {
        LiveStream liveStream = liveStreamRepository.findByStreamKey(streamKey);

        if (liveStream == null) {
            throw new NotFoundException(
                    ApiErrorCodes.NOT_FOUND,
                    "Live stream not found for stream key: " + streamKey
            );
        }

        return LiveStreamResponse.fromEntity(liveStream);
    }


    /**
     * Update viewer count for a live stream
     */
    @Transactional
    public void updateViewerCount(UUID id, int viewerCount) {
        LiveStream liveStream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        liveStream.setViewerCount(viewerCount);
        liveStream.setUpdatedAt(Instant.now());
        liveStreamRepository.save(liveStream);
    }

    /**
     * Mark that a live stream has an error
     */
    @Transactional
    public void markStreamError(UUID id) {
        LiveStream liveStream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        liveStream.setStatus(LiveStream.LiveStreamStatus.ERROR);
        liveStream.setUpdatedAt(Instant.now());
        liveStreamRepository.save(liveStream);
    }

    /**
     * Generate a unique stream key
     */
    private String generateStreamKey() {
        // Generate a random stream key (in practice, this would be more sophisticated)
        return "stream_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
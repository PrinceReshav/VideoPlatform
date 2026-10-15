package com.platform.video.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.video.entity.LiveStream;
import com.platform.common.entity.Video;
import com.platform.video.repository.LiveStreamRepository;
import com.platform.video.repository.VideoRepository;
import com.platform.common.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for converting live streams to VOD (Feature 31: Live Streaming Platform)
 * In a real implementation, this would integrate with actual transcoding systems
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamVodService {

    private final LiveStreamRepository liveStreamRepository;
    private final VideoRepository videoRepository;

    /**
     * Convert an ended live stream to a VOD video
     */
    @Transactional
    public UUID convertLiveStreamToVod(UUID liveStreamId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        // Check if live stream has ended
        if (liveStream.getStatus() != LiveStream.LiveStreamStatus.ENDING &&
            liveStream.getStatus() != LiveStream.LiveStreamStatus.OFFLINE) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Can only convert ended live streams to VOD");
        }

        // Check if already converted
        if (liveStream.getVodVideoId() != null) {
            return liveStream.getVodVideoId();
        }

        // Check if configured to save as VOD
        if (!liveStream.isSaveAsVod()) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Live stream is not configured to be saved as VOD");
        }

        // In a real implementation, this would:
        // 1. Retrieve the live stream recording from storage
        // 2. Transcode it to HLS/DASH format
        // 3. Save it to the video storage system
        // 4. Create a Video entity with appropriate metadata

        // For now, we'll create a basic Video entity with placeholder data
        Video vodVideo = new Video();
        vodVideo.setCreatorId(liveStream.getCreatorId());
        vodVideo.setChannelId(liveStream.getChannelId());
        vodVideo.setTitle(liveStream.getTitle() + " (Live Stream Recording)");
        vodVideo.setDescription("Recording of live stream: " + liveStream.getDescription());
        int durationSeconds = liveStream.getEndedAt() != null && liveStream.getStartedAt() != null
                ? (int) Math.min(Integer.MAX_VALUE, Math.max(0, java.time.Duration.between(liveStream.getStartedAt(), liveStream.getEndedAt()).getSeconds()))
                : 0;
        vodVideo.setDurationSeconds(durationSeconds);
        vodVideo.setSlug(SlugUtil.generateUniqueSlug(
                SlugUtil.slugify(vodVideo.getTitle()), videoRepository::existsBySlug));
        vodVideo.setVisibility(Video.Visibility.PRIVATE);
        vodVideo.setProcessingState(Video.ProcessingState.READY);
        vodVideo.setPublicationState(Video.PublicationState.DRAFT);

        // Set thumbnail to live stream preview if available
        if (liveStream.getPreviewUrl() != null) {
            vodVideo.setThumbnailUrl(liveStream.getPreviewUrl());
        }

        Video savedVideo = videoRepository.save(vodVideo);

        // Update the live stream with the VOD video ID
        liveStream.setVodVideoId(savedVideo.getId());
        liveStream.setUpdatedAt(Instant.now());
        liveStreamRepository.save(liveStream);

        log.info("Converted live stream {} to VOD video {}", liveStreamId, savedVideo.getId());

        return savedVideo.getId();
    }
}
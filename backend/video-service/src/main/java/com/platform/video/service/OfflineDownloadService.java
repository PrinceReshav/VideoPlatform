package com.platform.video.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.video.entity.OfflineDownload;
import com.platform.common.entity.Video;
import com.platform.video.repository.OfflineDownloadRepository;
import com.platform.video.repository.VideoRepository;
import com.platform.video.dto.OfflineDownloadRequest;
import com.platform.video.dto.OfflineDownloadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling offline downloads (Feature 34: Offline Downloads with DRM)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineDownloadService {

    private final OfflineDownloadRepository offlineDownloadRepository;
    private final VideoRepository videoRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Create a new offline download request
     * In a real implementation, this would trigger DRM license generation and content download
     */
    @Transactional
    public OfflineDownloadResponse createOfflineDownload(OfflineDownloadRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        // Verify the video exists and is accessible
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Video not found"));

        // Check if user already has an active download for this video
        OfflineDownload existingDownload = offlineDownloadRepository.findByUserIdAndVideoId(userId, request.getVideoId());
        if (existingDownload != null &&
            existingDownload.getStatus() == OfflineDownload.OfflineDownloadStatus.ACTIVE) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "User already has an active offline download for this video");
        }

        // Calculate expiration time (in a real system, this would come from DRM license)
        Instant expiresAt = request.getExpiresAt();
        if (expiresAt == null) {
            // Default to 30 days from now for offline downloads
            expiresAt = Instant.now().plusSeconds(30 * 24 * 60 * 60); // 30 days
        }

        OfflineDownload offlineDownload = new OfflineDownload();
        offlineDownload.setUserId(userId);
        offlineDownload.setVideoId(request.getVideoId());
        offlineDownload.setExpiresAt(expiresAt);
        offlineDownload.setStatus(OfflineDownload.OfflineDownloadStatus.ACTIVE);
        // In a real implementation, these would be set during the actual download process
        offlineDownload.setLocalIdentifier("placeholder-local-id-" + UUID.randomUUID());
        offlineDownload.setFileSizeBytes(0L); // Would be set after actual download
        offlineDownload.setCreatedAt(Instant.now());
        offlineDownload.setUpdatedAt(Instant.now());

        OfflineDownload savedDownload = offlineDownloadRepository.save(offlineDownload);
        log.info("Created offline download {} for user {} and video {}", savedDownload.getId(), userId, request.getVideoId());

        return OfflineDownloadResponse.fromEntity(savedDownload);
    }

    /**
     * Get an offline download by ID
     */
    @Transactional(readOnly = true)
    public OfflineDownloadResponse getOfflineDownload(UUID id) {
        OfflineDownload offlineDownload = offlineDownloadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Offline download not found"));

        // Check if user has permission to view this offline download
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!offlineDownload.getUserId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to view this offline download");
        }

        return OfflineDownloadResponse.fromEntity(offlineDownload);
    }

    /**
     * Get offline downloads by user ID
     */
    @Transactional(readOnly = true)
    public List<OfflineDownloadResponse> getOfflineDownloadsByUser(UUID userId) {
        // Check if user is requesting their own downloads
        UUID currentUserId = currentUserAccessor.getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You can only view your own offline downloads");
        }

        List<OfflineDownload> downloads = offlineDownloadRepository.findByUserId(userId);
        return downloads.stream()
                .map(OfflineDownloadResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Mark an offline download as played
     */
    @Transactional
    public OfflineDownloadResponse markAsPlayed(UUID id) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        OfflineDownload offlineDownload = offlineDownloadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Offline download not found"));

        // Check if user owns this offline download
        if (!offlineDownload.getUserId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to modify this offline download");
        }

        // Check if download is still active
        if (offlineDownload.getStatus() != OfflineDownload.OfflineDownloadStatus.ACTIVE) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Can only mark active offline downloads as played");
        }

        offlineDownload.setHasBeenPlayed(true);
        offlineDownload.setLastPlayedAt(Instant.now());
        offlineDownload.setPlayCount(offlineDownload.getPlayCount() + 1);
        offlineDownload.setUpdatedAt(Instant.now());

        OfflineDownload updatedDownload = offlineDownloadRepository.save(offlineDownload);
        log.info("Marked offline download {} as played by user {}", updatedDownload.getId(), userId);

        return OfflineDownloadResponse.fromEntity(updatedDownload);
    }

    /**
     * Expire an offline download (typically called by a background job)
     */
    @Transactional
    public void expireOfflineDownload(UUID id) {
        OfflineDownload offlineDownload = offlineDownloadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Offline download not found"));

        // Only expire active downloads
        if (offlineDownload.getStatus() == OfflineDownload.OfflineDownloadStatus.ACTIVE) {
            offlineDownload.setStatus(OfflineDownload.OfflineDownloadStatus.EXPIRED);
            offlineDownload.setUpdatedAt(Instant.now());
            offlineDownloadRepository.save(offlineDownload);
            log.info("Expired offline download {}", id);
        }
    }

    /**
     * Delete an offline download
     */
    @Transactional
    public void deleteOfflineDownload(UUID id) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        OfflineDownload offlineDownload = offlineDownloadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Offline download not found"));

        // Check if user owns this offline download
        if (!offlineDownload.getUserId().equals(userId)) {
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You don't have permission to delete this offline download");
        }

        // In a real implementation, this would also delete the actual downloaded files
        offlineDownload.setStatus(OfflineDownload.OfflineDownloadStatus.DELETED);
        offlineDownload.setUpdatedAt(Instant.now());
        offlineDownloadRepository.save(offlineDownload);
        log.info("Deleted offline download {} by user {}", id, userId);
    }

    /**
     * Process expired offline downloads (to be called by a scheduled job)
     */

    @Transactional
    public void processExpiredDownloads() {
        Instant now = Instant.now();

        List<OfflineDownload> expiredDownloads =
                offlineDownloadRepository.findExpiredDownloads(
                        now,
                        OfflineDownload.OfflineDownloadStatus.ACTIVE
                );

        for (OfflineDownload download : expiredDownloads) {
            download.setStatus(
                    OfflineDownload.OfflineDownloadStatus.EXPIRED
            );
            download.setUpdatedAt(now);
        }

        if (!expiredDownloads.isEmpty()) {
            offlineDownloadRepository.saveAll(expiredDownloads);
            log.info(
                    "Processed {} expired offline downloads",
                    expiredDownloads.size()
            );
        }
    }

}
package com.platform.upload.service;

import com.platform.user.entity.Channel;
import com.platform.user.entity.CreatorProfile;
import com.platform.user.repository.ChannelRepository;
import com.platform.user.repository.CreatorProfileRepository;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.storage.StorageProvider;
import com.platform.common.storage.StorageObject;
import com.platform.common.util.ChecksumUtil;
import com.platform.upload.dto.CompleteSessionRequest;
import com.platform.upload.dto.CreateSessionRequest;
import com.platform.upload.dto.SessionStatusResponse;
import com.platform.upload.entity.UploadChunk;
import com.platform.upload.entity.UploadSession;
import com.platform.common.event.UploadCompletedEvent;
import com.platform.upload.repository.UploadChunkRepository;
import com.platform.upload.repository.UploadSessionRepository;
import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadSessionService {

    private final UploadSessionRepository sessionRepository;
    private final UploadChunkRepository chunkRepository;
    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final StorageProvider storageProvider;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final CurrentUserAccessor currentUserAccessor;

    @Value("${upload.chunk-size-mb:256}")
    private long defaultChunkSizeMB;

    @Value("${upload.max-file-size-gb:50}")
    private long maxFileSizeGB;

    private static final long MIN_CHUNK_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_CHUNK_SIZE_BYTES = 256L * 1024 * 1024;
    private static final int MAX_MULTIPART_PARTS = 10_000;
    private static final int SESSION_TTL_HOURS = 24;

    @Transactional
    public SessionStatusResponse createSession(CreateSessionRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        // Validate file size
        long configuredMaxFileSizeBytes = maxFileSizeGB * 1024L * 1024L * 1024L;
        if (request.getTotalSize() <= 0 || request.getTotalSize() > configuredMaxFileSizeBytes) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_FILE_TOO_LARGE,
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "File size exceeds maximum allowed size of " + maxFileSizeGB + " GB");
        }

        if (request.getVideoId() != null) {
            Video existingVideo = videoRepository.findById(request.getVideoId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));
            if (!existingVideo.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
                throw new BusinessException(ApiErrorCodes.ACCESS_DENIED, org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to upload to this video");
            }
        }

        // Validate content type
        if (!isValidVideoMimeType(request.getContentType())) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_INVALID_MIME_TYPE,
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid video MIME type: " + request.getContentType());
        }

        long configuredDefaultChunkSize = defaultChunkSizeMB * 1024L * 1024L;
        long chunkSize = request.getChunkSize() != null ? request.getChunkSize() : configuredDefaultChunkSize;
        if (chunkSize < MIN_CHUNK_SIZE_BYTES || chunkSize > MAX_CHUNK_SIZE_BYTES) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_CHUNK_MISMATCH,
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Chunk size must be between 5 MB and 256 MB");
        }
        long totalChunksLong = (request.getTotalSize() + chunkSize - 1) / chunkSize;
        if (totalChunksLong > MAX_MULTIPART_PARTS) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_CHUNK_MISMATCH,
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "File would require more than 10,000 multipart chunks");
        }
        int totalChunks = (int) totalChunksLong;

        // Create MinIO multipart upload
        String objectKey = "uploads/" + userId + "/" + UUID.randomUUID() + "/" + request.getFileName();
        String minioUploadId = storageProvider.createMultipartUpload("videos", objectKey, request.getContentType());

        UploadSession session = new UploadSession();
        session.setUserId(userId);
        session.setVideoId(request.getVideoId());
        session.setFileName(request.getFileName());
        session.setContentType(request.getContentType());
        session.setTotalSize(request.getTotalSize());
        session.setChunkSize(chunkSize);
        session.setTotalChunks(totalChunks);
        session.setChecksum(request.getChecksum());
        session.setStatus(UploadSession.Status.INITIATED);
        session.setMinioUploadId(minioUploadId);
        session.setObjectKey(objectKey);
        session.setBucket("videos");
        session.setExpiresAt(Instant.now().plusSeconds(SESSION_TTL_HOURS * 3600));

        session = sessionRepository.save(session);
        log.info("Created upload session: {} for user: {}", session.getId(), userId);

        return mapToStatusResponse(session);
    }

    @Transactional
    public SessionStatusResponse uploadChunk(UUID sessionId, int partNumber, MultipartFile file) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.UPLOAD_SESSION_NOT_FOUND, "Upload session not found"));
        assertSessionOwner(session);

        // Validate session state
        if (session.getStatus() != UploadSession.Status.INITIATED && session.getStatus() != UploadSession.Status.IN_PROGRESS) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_INCOMPLETE,
                    org.springframework.http.HttpStatus.CONFLICT, "Session is not in progress");
        }

        if (session.isExpired()) {
            session.setStatus(UploadSession.Status.EXPIRED);
            sessionRepository.save(session);
            throw new BusinessException(ApiErrorCodes.UPLOAD_SESSION_EXPIRED,
                    org.springframework.http.HttpStatus.GONE, "Upload session expired");
        }

        // Validate part number
        if (partNumber < 1 || partNumber > session.getTotalChunks()) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_CHUNK_MISMATCH,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid part number");
        }

        // Check if already uploaded
        Optional<UploadChunk> existingChunk = chunkRepository.findBySessionIdAndPartNumber(sessionId, partNumber);
        if (existingChunk.isPresent()) {
            log.warn("Chunk {} already uploaded for session {}", partNumber, sessionId);
            return mapToStatusResponse(session);
        }

        // Validate chunk size (last chunk can be smaller)
        long expectedSize = (partNumber == session.getTotalChunks())
                ? session.getTotalSize() - (session.getTotalChunks() - 1) * session.getChunkSize()
                : session.getChunkSize();

        if (file.getSize() > expectedSize + 1024) { // Allow small overhead
            throw new BusinessException(ApiErrorCodes.UPLOAD_CHUNK_MISMATCH,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Chunk size mismatch");
        }

        // Verify checksum if provided
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            java.security.DigestInputStream digestStream = new java.security.DigestInputStream(inputStream, digest);

            // Stream the multipart chunk directly to object storage. This avoids
            // materializing a 256 MB chunk as a JVM byte[] under concurrent load.
            String etag = storageProvider.uploadPart("videos", session.getObjectKey(),
                    session.getMinioUploadId(), partNumber, digestStream, file.getSize());
            String checksum = java.util.HexFormat.of().formatHex(digest.digest());

            UploadChunk chunk = new UploadChunk();
            chunk.setSessionId(sessionId);
            chunk.setPartNumber(partNumber);
            chunk.setSize(file.getSize());
            chunk.setEtag(etag);
            chunk.setChecksum(checksum);
            chunk.setMinioPartNumber(partNumber);
            chunkRepository.save(chunk);

            // Update session
            session.setStatus(UploadSession.Status.IN_PROGRESS);
            session.setUploadedChunks(session.getUploadedChunks() + 1);
            sessionRepository.save(session);

            log.debug("Uploaded chunk {} for session {}", partNumber, sessionId);
            return mapToStatusResponse(session);

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to read chunk data", e);
            throw new BusinessException(ApiErrorCodes.UPLOAD_FAILED,
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process chunk");
        }
    }

    @Transactional
    public SessionStatusResponse completeSession(UUID sessionId, CompleteSessionRequest request) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.UPLOAD_SESSION_NOT_FOUND, "Upload session not found"));
        assertSessionOwner(session);

        if (session.getStatus() == UploadSession.Status.COMPLETED) {
            return mapToStatusResponse(session);
        }

        if (session.getStatus() != UploadSession.Status.IN_PROGRESS) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_INCOMPLETE,
                    org.springframework.http.HttpStatus.CONFLICT, "Session not in progress");
        }

        // Verify all parts uploaded
        Set<Integer> uploadedParts = chunkRepository.findBySessionIdOrderByPartNumberAsc(sessionId).stream()
                .map(UploadChunk::getPartNumber)
                .collect(Collectors.toSet());

        if (uploadedParts.size() != session.getTotalChunks()) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_INCOMPLETE,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Not all chunks uploaded");
        }

        // Verify part order
        for (int i = 1; i <= session.getTotalChunks(); i++) {
            if (!uploadedParts.contains(i)) {
                throw new BusinessException(ApiErrorCodes.UPLOAD_CHUNK_MISMATCH,
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Missing chunk: " + i);
            }
        }

        // Complete multipart upload in MinIO
        List<StorageProvider.PartETag> minioParts = chunkRepository.findBySessionIdOrderByPartNumberAsc(sessionId).stream()
                .map(chunk -> new StorageProvider.PartETag(chunk.getPartNumber(), chunk.getEtag()))
                .toList();

        StorageObject result = storageProvider.completeMultipartUpload(
                "videos", session.getObjectKey(), session.getMinioUploadId(), minioParts);

        // Verify the completed object against the client SHA-256 when supplied.
        if (request.getChecksum() != null && !request.getChecksum().isBlank()) {
            var completedStream = storageProvider.getObjectStream("videos", result.getKey())
                    .orElseThrow(() -> new BusinessException(ApiErrorCodes.UPLOAD_CHECKSUM_MISMATCH,
                            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Completed object could not be read for checksum verification"));
            try (completedStream) {
                String actualChecksum = ChecksumUtil.sha256(completedStream);
                if (!request.getChecksum().equalsIgnoreCase(actualChecksum)) {
                    storageProvider.deleteObject("videos", result.getKey());
                    throw new BusinessException(ApiErrorCodes.UPLOAD_CHECKSUM_MISMATCH,
                            org.springframework.http.HttpStatus.BAD_REQUEST, "File checksum mismatch");
                }
                result.setChecksum(actualChecksum);
            } catch (java.io.IOException e) {
                throw new BusinessException(ApiErrorCodes.UPLOAD_CHECKSUM_MISMATCH,
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Checksum verification failed");
            }
        }

        // Update session
        session.setStatus(UploadSession.Status.COMPLETED);
        session.setCompletedAt(Instant.now());
        sessionRepository.save(session);

        // Create or update video record
        Video video;
        if (session.getVideoId() != null) {
            video = videoRepository.findById(session.getVideoId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));
            video.setSourceObjectKey(result.getKey());
            video.setSourceFileSize(result.getSize());
            video.setSourceChecksum(result.getChecksum());
            video.setProcessingState(Video.ProcessingState.UPLOADED);
            video.setModerationState(Video.ModerationState.PENDING);
        } else {
            // Create minimal video record
            video = new Video();
            video.setCreatorId(session.getUserId());
            video.setChannelId(getChannelIdForUser(session.getUserId()));
            video.setTitle(extractTitleFromFilename(session.getFileName()));
            video.setSlug(com.platform.common.util.SlugUtil.slugify(extractTitleFromFilename(session.getFileName())));
            video.setVisibility(Video.Visibility.PRIVATE);
            video.setProcessingState(Video.ProcessingState.UPLOADED);
            video.setModerationState(Video.ModerationState.PENDING);
            video.setSourceObjectKey(result.getKey());
            video.setSourceFileSize(result.getSize());
            video.setSourceChecksum(result.getChecksum());
        }
        videoRepository.save(video);

        // Update session with video ID
        session.setVideoId(video.getId());
        sessionRepository.save(session);

        // Publish completion event
        publishUploadCompletedEvent(video, session, result);

        log.info("Completed upload session: {} -> video: {}", sessionId, video.getId());
        return mapToStatusResponse(session);
    }

    @Transactional
    public void cancelSession(UUID sessionId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.UPLOAD_SESSION_NOT_FOUND, "Upload session not found"));

        if (!session.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to cancel this upload");
        }

        if (session.getStatus() == UploadSession.Status.COMPLETED) {
            throw new BusinessException(ApiErrorCodes.UPLOAD_INCOMPLETE,
                    org.springframework.http.HttpStatus.CONFLICT, "Upload already completed");
        }

        // Abort multipart upload in MinIO
        try {
            storageProvider.abortMultipartUpload("videos", session.getObjectKey(), session.getMinioUploadId());
        } catch (Exception e) {
            log.warn("Failed to abort multipart upload in MinIO: {}", e.getMessage());
        }

        // Delete chunks
        chunkRepository.deleteBySessionId(sessionId);

        // Update session
        session.setStatus(UploadSession.Status.CANCELLED);
        sessionRepository.save(session);

        log.info("Cancelled upload session: {}", sessionId);
    }

    @Transactional(readOnly = true)
    public SessionStatusResponse getSessionStatus(UUID sessionId) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.UPLOAD_SESSION_NOT_FOUND, "Upload session not found"));

        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!session.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to view this session");
        }

        return mapToStatusResponse(session);
    }

    @Transactional(readOnly = true)
    public List<Integer> getUploadedPartNumbers(UUID sessionId) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.UPLOAD_SESSION_NOT_FOUND, "Upload session not found"));
        assertSessionOwner(session);
        return chunkRepository.findBySessionIdOrderByPartNumberAsc(sessionId).stream()
                .map(UploadChunk::getPartNumber)
                .toList();
    }

    @Transactional(readOnly = true)
    public com.platform.common.dto.PageResponse<SessionStatusResponse> getMySessions(int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        var sessions = sessionRepository.findByUserId(userId, PageRequest.of(page, size));
        return com.platform.common.dto.PageResponse.of(
                sessions.getContent().stream().map(this::mapToStatusResponse).toList(),
                page, size, sessions.getTotalElements());
    }

    @Transactional
    public void cleanupExpiredSessions() {
        List<UploadSession> expiredSessions = sessionRepository.findExpiredSessions(Instant.now());
        for (UploadSession session : expiredSessions) {
            log.info("Cleaning up expired session: {}", session.getId());
            try {
                storageProvider.abortMultipartUpload("videos", session.getObjectKey(), session.getMinioUploadId());
            } catch (Exception e) {
                log.warn("Failed to abort multipart upload for expired session {}: {}", session.getId(), e.getMessage());
            }
            chunkRepository.deleteBySessionId(session.getId());
            session.setStatus(UploadSession.Status.EXPIRED);
            sessionRepository.save(session);
        }
    }

    private void publishUploadCompletedEvent(Video video, UploadSession session, StorageObject result) {
        UploadCompletedEvent event = UploadCompletedEvent.builder()
                .videoId(video.getId())
                .sessionId(session.getId())
                .creatorId(session.getUserId())
                .objectKey(result.getKey())
                .fileSize(result.getSize())
                .checksum(result.getChecksum())
                .createdAt(Instant.now())
                .build();

        EventEnvelope<UploadCompletedEvent> envelope = EventEnvelope.create(
                "video.uploaded", "v1", event, "upload-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_UPLOADED, video.getId().toString(), envelope);
    }

    private SessionStatusResponse mapToStatusResponse(UploadSession session) {
        List<Integer> uploadedParts = getUploadedPartNumbers(session.getId());

        return SessionStatusResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .videoId(session.getVideoId())
                .fileName(session.getFileName())
                .contentType(session.getContentType())
                .totalSize(session.getTotalSize())
                .chunkSize(session.getChunkSize())
                .totalChunks(session.getTotalChunks())
                .uploadedChunks(session.getUploadedChunks())
                .progressPercent(session.getProgressPercent())
                .status(session.getStatus().name())
                .minioUploadId(session.getMinioUploadId())
                .objectKey(session.getObjectKey())
                .bucket(session.getBucket())
                .expiresAt(session.getExpiresAt())
                .completedAt(session.getCompletedAt())
                .errorMessage(session.getErrorMessage())
                .uploadedPartNumbers(uploadedParts)
                .build();
    }

    private void assertSessionOwner(UploadSession session) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        if (!session.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED, org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to access this upload session");
        }
    }

    private boolean isValidVideoMimeType(String contentType) {
        return contentType != null && (
                contentType.equals("video/mp4") ||
                contentType.equals("video/quicktime") ||
                contentType.equals("video/x-matroska") ||
                contentType.equals("video/webm") ||
                contentType.equals("video/x-msvideo") ||
                contentType.equals("video/mpeg")
        );
    }


    private UUID getChannelIdForUser(UUID userId) {
        UUID creatorProfileId = creatorProfileRepository.findByUserId(userId)
                .map(com.platform.user.entity.CreatorProfile::getId)
                .orElseThrow(() -> new BusinessException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND,
                        org.springframework.http.HttpStatus.FORBIDDEN, "Creator profile not found"));
        return channelRepository.findByCreatorProfileId(creatorProfileId)
                .map(com.platform.user.entity.Channel::getId)
                .orElseThrow(() -> new BusinessException(ApiErrorCodes.CHANNEL_NOT_FOUND,
                        org.springframework.http.HttpStatus.FORBIDDEN, "Creator channel not found"));
    }

    private String extractTitleFromFilename(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
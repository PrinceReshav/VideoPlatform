package com.platform.video.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.entity.ProcessingJob;
import com.platform.common.entity.Video;
import com.platform.common.entity.VideoFile;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.video.repository.ProcessingJobRepository;
import com.platform.video.repository.VideoFileRepository;
import com.platform.video.repository.VideoRepository;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.storage.StorageProvider;
import com.platform.common.storage.StorageObject;
import com.platform.common.util.ChecksumUtil;
import com.platform.common.util.SlugUtil;
import com.platform.video.dto.CreateVideoRequest;
import com.platform.video.dto.UpdateVideoRequest;
import com.platform.video.dto.VideoDetailResponse;
import com.platform.video.dto.VideoResponse;
import com.platform.common.event.VideoDeletedEvent;
import com.platform.common.event.VideoPublishedEvent;
import com.platform.common.event.VideoUnpublishedEvent;
import com.platform.video.listener.UploadCompletedListener;
import com.platform.video.projection.VideoListProjection;
import com.platform.user.entity.Category;
import com.platform.user.entity.Channel;
import com.platform.user.entity.Genre;
import com.platform.user.entity.Language;
import com.platform.user.repository.CategoryRepository;
import com.platform.user.repository.ChannelRepository;
import com.platform.user.repository.CreatorProfileRepository;
import com.platform.user.repository.GenreRepository;
import com.platform.user.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoFileRepository videoFileRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final LanguageRepository languageRepository;
    private final StorageProvider storageProvider;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional
    public VideoResponse createVideo(CreateVideoRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        // Verify creator profile exists
        var creatorProfile = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND,
                        org.springframework.http.HttpStatus.FORBIDDEN, "User is not a creator"));

        // Verify channel exists
        Channel channel = channelRepository.findByCreatorProfileId(creatorProfile.getId())
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CHANNEL_NOT_FOUND, "Channel not found"));

        // Validate category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));

        // Validate genre if provided
        Genre genre = null;
        if (request.getGenreId() != null) {
            genre = genreRepository.findById(request.getGenreId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Genre not found"));
        }

        // Validate language if provided
        Language language = null;
        if (request.getLanguageId() != null) {
            language = languageRepository.findById(request.getLanguageId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));
        }

        String slug = SlugUtil.slugify(request.getTitle());
        slug = SlugUtil.generateUniqueSlug(slug, s -> videoRepository.existsBySlug(s));

        Video video = new Video();
        video.setCreatorId(userId);
        video.setChannelId(channel.getId());
        video.setTitle(request.getTitle());
        video.setSlug(slug);
        video.setDescription(request.getDescription());
        video.setTags(request.getTags());
        video.setCategoryId(category.getId());
        video.setGenreId(genre != null ? genre.getId() : null);
        video.setLanguageId(language != null ? language.getId() : null);
        video.setVisibility(Video.Visibility.valueOf(request.getVisibility()));
        video.setAllowComments(request.isAllowComments());
        video.setAllowEmbedding(request.isAllowEmbedding());
        video.setAgeRestricted(request.isAgeRestricted());
        video.setProcessingState(Video.ProcessingState.UPLOADING);
        video.setPublicationState(Video.PublicationState.DRAFT);

        // If upload session provided, link it
        if (request.getUploadSessionId() != null) {
            // The upload completed listener will handle the rest
        }

        video = videoRepository.save(video);
        log.info("Created video metadata: {} ({})", video.getTitle(), video.getId());

        return mapToVideoResponse(video, creatorProfile, channel);
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse getVideoDetail(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.isDeleted()) {
            throw new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found");
        }

        // Check access
        checkVideoAccess(video);

        return mapToVideoDetailResponse(video);
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse getVideoBySlug(String slug) {
        Video video = videoRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.isDeleted()) {
            throw new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found");
        }

        checkVideoAccess(video);
        return mapToVideoDetailResponse(video);
    }

    @Transactional
    public VideoResponse updateVideo(UUID videoId, UpdateVideoRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.isDeleted()) {
            throw new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found");
        }

        // Check ownership
        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to update this video");
        }

        if (request.getTitle() != null) {
            String slug = SlugUtil.slugify(request.getTitle());
            slug = SlugUtil.generateUniqueSlug(slug, s -> videoRepository.existsBySlug(s));
            video.setTitle(request.getTitle());
            video.setSlug(slug);
        }

        if (request.getDescription() != null) video.setDescription(request.getDescription());
        if (request.getTags() != null) video.setTags(request.getTags());
        if (request.getVisibility() != null) video.setVisibility(Video.Visibility.valueOf(request.getVisibility()));
        if (request.getAllowComments() != null) video.setAllowComments(request.getAllowComments());
        if (request.getAllowEmbedding() != null) video.setAllowEmbedding(request.getAllowEmbedding());
        if (request.getAgeRestricted() != null) video.setAgeRestricted(request.getAgeRestricted());
        if (request.getThumbnailUrl() != null) video.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getPreviewUrl() != null) video.setPreviewUrl(request.getPreviewUrl());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));
            video.setCategoryId(category.getId());
        }

        if (request.getGenreId() != null) {
            Genre genre = genreRepository.findById(request.getGenreId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Genre not found"));
            video.setGenreId(genre != null ? genre.getId() : null);
        } else if (request.getGenreId() != null && request.getGenreId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            video.setGenreId(null);
        }

        if (request.getLanguageId() != null) {
            Language language = languageRepository.findById(request.getLanguageId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));
            video.setLanguageId(language != null ? language.getId() : null);
        } else if (request.getLanguageId() != null && request.getLanguageId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            video.setLanguageId(null);
        }

        video = videoRepository.save(video);
        log.info("Updated video: {}", video.getId());

        return mapToVideoResponse(video, null, null);
    }

    @Transactional
    public void deleteVideo(UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to delete this video");
        }

        // Soft delete
        video.markDeleted(userId);
        videoRepository.save(video);

        // Delete associated files from storage
        try {
            List<VideoFile> files = videoFileRepository.findByVideoId(videoId);
            for (VideoFile file : files) {
                storageProvider.deleteObject(file.getBucket(), file.getObjectKey());
            }
            videoFileRepository.deleteByVideoId(videoId);
        } catch (Exception e) {
            log.warn("Failed to delete video files from storage: {}", e.getMessage());
        }

        // Publish deletion event
        publishVideoDeletedEvent(video, userId);

        log.info("Deleted video: {}", videoId);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${video-service.scheduling.fixed-delay-ms:30000}")
    public void publishDueScheduledVideos() {
        for (Video video : videoRepository.findScheduledVideosDue(Instant.now())) {
            try {
                video.publish();
                videoRepository.save(video);
                publishVideoPublishedEvent(video);
                log.info("Published scheduled video {}", video.getId());
            } catch (Exception e) {
                log.error("Failed to publish scheduled video {}", video.getId(), e);
            }
        }
    }

    @Transactional
    public VideoResponse publishVideo(UUID videoId, com.platform.video.dto.PublishRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to publish this video");
        }

        if (video.getProcessingState() != Video.ProcessingState.READY) {
            throw new BusinessException(ApiErrorCodes.VIDEO_PROCESSING,
                    org.springframework.http.HttpStatus.CONFLICT, "Video is not ready for publishing");
        }

        if (request.isScheduled() && request.getScheduledAt() != null) {
            video.setPublicationState(Video.PublicationState.SCHEDULED);
            video.setPublishedAt(request.getScheduledAt());
        } else {
            video.publish();
            publishVideoPublishedEvent(video);
        }

        videoRepository.save(video);
        log.info("Published video: {}", videoId);

        return mapToVideoResponse(video, null, null);
    }

    @Transactional
    public VideoResponse unpublishVideo(UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to unpublish this video");
        }

        video.unpublish();
        publishVideoUnpublishedEvent(video);
        videoRepository.save(video);

        log.info("Unpublished video: {}", videoId);
        return mapToVideoResponse(video, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<com.platform.video.projection.VideoListProjection> getMyVideos(int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<com.platform.video.projection.VideoListProjection> videoPage =
                videoRepository.findByCreatorIdProjected(userId, pageable);
        return PageResponse.of(videoPage.getContent(), page, size, videoPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<com.platform.video.projection.VideoListProjection> getChannelVideos(UUID channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.platform.video.projection.VideoListProjection> videoPage =
                videoRepository.findByChannelIdProjected(channelId, pageable);
        return PageResponse.of(videoPage.getContent(), page, size, videoPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<com.platform.video.projection.VideoListProjection> getPublishedVideos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.platform.video.projection.VideoListProjection> videoPage =
                videoRepository.findAllProjected(pageable);
        return PageResponse.of(videoPage.getContent(), page, size, videoPage.getTotalElements());
    }

    @Transactional
    public void incrementViewCount(UUID videoId) {
        videoRepository.incrementViewCount(videoId);
    }

    @Transactional
    public void incrementLikeCount(UUID videoId) {
        videoRepository.incrementLikeCount(videoId);
    }

    @Transactional
    public void decrementLikeCount(UUID videoId) {
        videoRepository.decrementLikeCount(videoId);
    }

    @Transactional
    public void incrementCommentCount(UUID videoId) {
        videoRepository.incrementCommentCount(videoId);
    }

    @Transactional
    public void decrementCommentCount(UUID videoId) {
        videoRepository.decrementCommentCount(videoId);
    }

    public void saveVideoFile(UUID videoId, com.platform.common.entity.VideoFileInfo fileInfo) {
        VideoFile videoFile = new VideoFile();
        videoFile.setVideoId(videoId);
        videoFile.setFileType(fileInfo.getFileType());
        videoFile.setResolution(fileInfo.getResolution());
        videoFile.setObjectKey(fileInfo.getObjectKey());
        videoFile.setBucket(fileInfo.getBucket());
        videoFile.setFileSize(fileInfo.getFileSize());
        videoFile.setContentType(fileInfo.getContentType());
        videoFile.setChecksum(fileInfo.getChecksum());
        videoFile.setDurationSeconds(fileInfo.getDurationSeconds());
        videoFile.setBitrate(fileInfo.getBitrate());
        videoFile.setWidth(fileInfo.getWidth());
        videoFile.setHeight(fileInfo.getHeight());
        videoFile.setCodec(fileInfo.getCodec());
        videoFile.setSegmentIndex(fileInfo.getSegmentIndex());
        videoFile.setMaster(fileInfo.isMaster());

        videoFileRepository.save(videoFile);
    }

    public String generatePresignedHlsUrl(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.getHlsManifestKey() == null) {
            throw new BusinessException(ApiErrorCodes.VIDEO_PROCESSING,
                    org.springframework.http.HttpStatus.CONFLICT, "HLS manifest not available");
        }

        return storageProvider.generatePresignedGetUrl("hls", video.getHlsManifestKey(), 4 * 60 * 60); // 4 hours
    }

    public String generatePresignedSegmentUrl(String objectKey) {
        return storageProvider.generatePresignedGetUrl("hls", objectKey, 4 * 60 * 60);
    }

    private void checkVideoAccess(Video video) {
        UUID currentUserId = currentUserAccessor.getCurrentUserId();

        if (video.isPublished()) {
            return; // Public access
        }

        // Owner or admin can access
        if (video.getCreatorId().equals(currentUserId) || currentUserAccessor.isAdmin()) {
            return;
        }

        // For unlisted, anyone with link can access
        if (video.getVisibility() == Video.Visibility.UNLISTED) {
            return;
        }

        throw new BusinessException(ApiErrorCodes.VIDEO_PRIVATE,
                org.springframework.http.HttpStatus.FORBIDDEN, "Video is private");
    }

    private void publishVideoPublishedEvent(Video video) {
        Channel channel = channelRepository.findById(video.getChannelId()).orElse(null);
        VideoPublishedEvent event = VideoPublishedEvent.builder()
                .videoId(video.getId())
                .creatorId(video.getCreatorId())
                .channelId(video.getChannelId())
                .title(video.getTitle())
                .slug(video.getSlug())
                .description(video.getDescription())
                .tags(video.getTags())
                .genreId(video.getGenreId())
                .genreName(video.getGenreId() != null ? genreRepository.findById(video.getGenreId()).map(Genre::getName).orElse(null) : null)
                .categoryId(video.getCategoryId())
                .categoryName(video.getCategoryId() != null ? categoryRepository.findById(video.getCategoryId()).map(Category::getName).orElse(null) : null)
                .languageId(video.getLanguageId())
                .languageName(video.getLanguageId() != null ? languageRepository.findById(video.getLanguageId()).map(Language::getName).orElse(null) : null)
                .durationSeconds(video.getDurationSeconds())
                .thumbnailUrl(video.getThumbnailUrl())
                .visibility(video.getVisibility().name())
                .availableResolutions(video.getAvailableResolutions())
                .publishedAt(video.getPublishedAt())
                .channelName(channel != null ? channel.getName() : null)
                .channelSlug(channel != null ? channel.getSlug() : null)
                .build();

        EventEnvelope<VideoPublishedEvent> envelope = EventEnvelope.create(
                "video.published", "v1", event, "video-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_PUBLISHED, video.getId().toString(), envelope);
    }

    private void publishVideoUnpublishedEvent(Video video) {
        VideoUnpublishedEvent event = VideoUnpublishedEvent.builder()
                .videoId(video.getId())
                .reason("Unpublished by creator")
                .unpublishedAt(Instant.now())
                .build();

        EventEnvelope<VideoUnpublishedEvent> envelope = EventEnvelope.create(
                "video.unpublished", "v1", event, "video-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_UNPUBLISHED, video.getId().toString(), envelope);
    }

    private void publishVideoDeletedEvent(Video video, UUID deletedBy) {
        VideoDeletedEvent event = VideoDeletedEvent.builder()
                .videoId(video.getId())
                .deletedBy(deletedBy)
                .deletedAt(Instant.now())
                .build();

        EventEnvelope<VideoDeletedEvent> envelope = EventEnvelope.create(
                "video.deleted", "v1", event, "video-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_DELETED, video.getId().toString(), envelope);
    }

    public VideoResponse mapAdminResponse(Video video) {
        return mapToVideoResponse(video, null, null);
    }

    private VideoResponse mapToVideoResponse(Video video, com.platform.user.entity.CreatorProfile creatorProfile, Channel channel) {
        return VideoResponse.builder()
                .id(video.getId())
                .creatorId(video.getCreatorId())
                .channelId(video.getChannelId())
                .title(video.getTitle())
                .slug(video.getSlug())
                .description(video.getDescription())
                .tags(video.getTags())
                .genreId(video.getGenreId())
                .genreName(video.getGenreId() != null ? genreRepository.findById(video.getGenreId()).map(Genre::getName).orElse(null) : null)
                .categoryId(video.getCategoryId())
                .categoryName(video.getCategoryId() != null ? categoryRepository.findById(video.getCategoryId()).map(Category::getName).orElse(null) : null)
                .languageId(video.getLanguageId())
                .languageName(video.getLanguageId() != null ? languageRepository.findById(video.getLanguageId()).map(Language::getName).orElse(null) : null)
                .durationSeconds(video.getDurationSeconds())
                .thumbnailUrl(video.getThumbnailUrl())
                .previewUrl(video.getPreviewUrl())
                .visibility(video.getVisibility().name())
                .processingState(video.getProcessingState().name())
                .publicationState(video.getPublicationState().name())
                .publishedAt(video.getPublishedAt())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .shareCount(video.getShareCount())
                .allowComments(video.isAllowComments())
                .allowEmbedding(video.isAllowEmbedding())
                .ageRestricted(video.isAgeRestricted())
                .blocked(video.isBlocked())
                .blockedReason(video.getBlockedReason())
                .availableResolutions(video.getAvailableResolutions())
                .hlsManifestUrl(video.getHlsManifestKey() != null ? generatePresignedHlsUrl(video.getId()) : null)
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .creatorName(creatorProfile != null ? creatorProfile.getChannelName() : null)
                .channelName(channel != null ? channel.getName() : null)
                .channelSlug(channel != null ? channel.getSlug() : null)
                .build();
    }

    private VideoDetailResponse mapToVideoDetailResponse(Video video) {
        // Get video files for resolution details
        List<VideoFile> files = videoFileRepository.findByVideoId(video.getId());
        List<VideoDetailResponse.VideoResolutionInfo> resolutionDetails = files.stream()
                .filter(f -> f.getFileType() == com.platform.common.entity.FileType.HLS_MANIFEST && !f.isMaster())
                .map(f -> VideoDetailResponse.VideoResolutionInfo.builder()
                        .resolution(f.getResolution())
                        .width(f.getWidth() != null ? f.getWidth().toString() : null)
                        .height(f.getHeight() != null ? f.getHeight().toString() : null)
                        .bitrate(f.getBitrate() != null ? f.getBitrate().longValue() : null)
                        .codec(f.getCodec())
                        .manifestUrl(storageProvider.generatePresignedGetUrl(f.getBucket(), f.getObjectKey(), 4 * 60 * 60))
                        .build())
                .toList();

        return VideoDetailResponse.builder()
                .id(video.getId())
                .creatorId(video.getCreatorId())
                .channelId(video.getChannelId())
                .title(video.getTitle())
                .slug(video.getSlug())
                .description(video.getDescription())
                .tags(video.getTags())
                .genreId(video.getGenreId())
                .genreName(video.getGenreId() != null ? genreRepository.findById(video.getGenreId()).map(Genre::getName).orElse(null) : null)
                .categoryId(video.getCategoryId())
                .categoryName(video.getCategoryId() != null ? categoryRepository.findById(video.getCategoryId()).map(Category::getName).orElse(null) : null)
                .languageId(video.getLanguageId())
                .languageName(video.getLanguageId() != null ? languageRepository.findById(video.getLanguageId()).map(Language::getName).orElse(null) : null)
                .durationSeconds(video.getDurationSeconds())
                .thumbnailUrl(video.getThumbnailUrl())
                .previewUrl(video.getPreviewUrl())
                .visibility(video.getVisibility().name())
                .processingState(video.getProcessingState().name())
                .publicationState(video.getPublicationState().name())
                .publishedAt(video.getPublishedAt())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .shareCount(video.getShareCount())
                .allowComments(video.isAllowComments())
                .allowEmbedding(video.isAllowEmbedding())
                .ageRestricted(video.isAgeRestricted())
                .blocked(video.isBlocked())
                .blockedReason(video.getBlockedReason())
                .availableResolutions(video.getAvailableResolutions())
                .hlsManifestUrl(video.getHlsManifestKey() != null ? generatePresignedHlsUrl(video.getId()) : null)
                .resolutionDetails(resolutionDetails)
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .creatorName(null) // Would need to fetch
                .channelName(null)
                .channelSlug(null)
                .channelAvatarUrl(null)
                .isSubscribed(false)
                .isLiked(false)
                .watchProgress(null)
                .build();
    }
}
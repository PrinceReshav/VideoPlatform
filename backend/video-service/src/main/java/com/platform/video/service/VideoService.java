
package com.platform.video.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.entity.FileType;
import com.platform.common.entity.ProcessingJob;
import com.platform.common.entity.Video;
import com.platform.common.entity.VideoFile;
import com.platform.common.entity.VideoFileInfo;
import com.platform.common.event.VideoDeletedEvent;
import com.platform.common.event.VideoPublishedEvent;
import com.platform.common.event.VideoUnpublishedEvent;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.storage.StorageProvider;
import com.platform.common.util.SlugUtil;
import com.platform.video.dto.*;
import com.platform.video.projection.VideoListProjection;
import com.platform.video.repository.ProcessingJobRepository;
import com.platform.video.repository.VideoFileRepository;
import com.platform.video.repository.VideoRepository;
import com.platform.video.repository.VideoVersionRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private static final UUID CLEAR_OPTIONAL_RELATIONSHIP =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final VideoVersionRepository videoVersionRepository;
    private final VideoVersionService videoVersionService;
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

        var creatorProfile = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND,
                        HttpStatus.FORBIDDEN,
                        "User is not a creator"));

        Channel channel = channelRepository.findByCreatorProfileId(creatorProfile.getId())
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.CHANNEL_NOT_FOUND, "Channel not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));

        Genre genre = null;
        if (request.getGenreId() != null) {
            genre = genreRepository.findById(request.getGenreId())
                    .orElseThrow(() -> new NotFoundException(
                            ApiErrorCodes.RESOURCE_NOT_FOUND, "Genre not found"));
        }

        Language language = null;
        if (request.getLanguageId() != null) {
            language = languageRepository.findById(request.getLanguageId())
                    .orElseThrow(() -> new NotFoundException(
                            ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));
        }

        String slug = SlugUtil.slugify(request.getTitle());
        slug = SlugUtil.generateUniqueSlug(slug, videoRepository::existsBySlug);

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

        // Upload completion is handled by the upload workflow/listener.
        // The upload session is intentionally not linked here.

        video = videoRepository.save(video);

        log.info("Created video metadata: {} ({})", video.getTitle(), video.getId());

        return mapToVideoResponse(video, creatorProfile, channel);
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse getVideoDetail(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.isDeleted()) {
            throw new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found");
        }

        checkVideoAccess(video);
        return mapToVideoDetailResponse(video);
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse getVideoBySlug(String slug) {
        Video video = videoRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

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
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.isDeleted()) {
            throw new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found");
        }

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to update this video");
        }

        boolean hasChanges = false;

        if (request.getTitle() != null) {
            final String currentSlug = video.getSlug();
            String baseSlug = SlugUtil.slugify(request.getTitle());

            String slug = SlugUtil.generateUniqueSlug(
                    baseSlug,
                    candidate -> !candidate.equals(currentSlug)
                            && videoRepository.existsBySlug(candidate)
            );

            video.setTitle(request.getTitle());
            video.setSlug(slug);
            hasChanges = true;
        }

        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
            hasChanges = true;
        }

        if (request.getTags() != null) {
            video.setTags(request.getTags());
            hasChanges = true;
        }

        if (request.getVisibility() != null) {
            video.setVisibility(Video.Visibility.valueOf(request.getVisibility()));
            hasChanges = true;
        }

        if (request.getAllowComments() != null) {
            video.setAllowComments(request.getAllowComments());
            hasChanges = true;
        }

        if (request.getAllowEmbedding() != null) {
            video.setAllowEmbedding(request.getAllowEmbedding());
            hasChanges = true;
        }

        if (request.getAgeRestricted() != null) {
            video.setAgeRestricted(request.getAgeRestricted());
            hasChanges = true;
        }

        if (request.getThumbnailUrl() != null) {
            video.setThumbnailUrl(request.getThumbnailUrl());
            hasChanges = true;
        }

        if (request.getPreviewUrl() != null) {
            video.setPreviewUrl(request.getPreviewUrl());
            hasChanges = true;
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(
                            ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));

            video.setCategoryId(category.getId());
            hasChanges = true;
        }

        /*
         * Null means "leave unchanged".
         * The all-zero UUID is retained as the explicit clear sentinel used
         * by the existing request contract.
         */
        if (request.getGenreId() != null) {
            if (CLEAR_OPTIONAL_RELATIONSHIP.equals(request.getGenreId())) {
                video.setGenreId(null);
            } else {
                Genre genre = genreRepository.findById(request.getGenreId())
                        .orElseThrow(() -> new NotFoundException(
                                ApiErrorCodes.RESOURCE_NOT_FOUND, "Genre not found"));

                video.setGenreId(genre.getId());
            }
            hasChanges = true;
        }

        if (request.getLanguageId() != null) {
            if (CLEAR_OPTIONAL_RELATIONSHIP.equals(request.getLanguageId())) {
                video.setLanguageId(null);
            } else {
                Language language = languageRepository.findById(request.getLanguageId())
                        .orElseThrow(() -> new NotFoundException(
                                ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));

                video.setLanguageId(language.getId());
            }
            hasChanges = true;
        }

        video = videoRepository.save(video);
        log.info("Updated video: {}", video.getId());

        if (hasChanges) {
            String changeSummary = buildChangeSummary(request);
            videoVersionService.createVideoVersion(videoId, changeSummary, userId);
            log.info("Created new version for video: {}", videoId);
        }

        return mapToVideoResponse(video, null, null);
    }

    @Transactional
    public void deleteVideo(UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to delete this video");
        }

        if (video.isDeleted()) {
            return;
        }

        video.markDeleted(userId);
        videoRepository.save(video);

        try {
            List<VideoFile> files = videoFileRepository.findByVideoId(videoId);
            for (VideoFile file : files) {
                storageProvider.deleteObject(file.getBucket(), file.getObjectKey());
            }
            videoFileRepository.deleteByVideoId(videoId);
        } catch (Exception e) {
            log.warn("Failed to delete video files from storage for {}: {}",
                    videoId, e.getMessage());
        }

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
    public VideoResponse publishVideo(UUID videoId, PublishRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.isDeleted()) {
            throw new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found");
        }

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to publish this video");
        }

        if (video.getProcessingState() != Video.ProcessingState.READY) {
            throw new BusinessException(
                    ApiErrorCodes.VIDEO_PROCESSING,
                    HttpStatus.CONFLICT,
                    "Video is not ready for publishing");
        }

        boolean wasPublished =
                video.getPublicationState() == Video.PublicationState.PUBLISHED;

        boolean scheduled = request.isScheduled() && request.getScheduledAt() != null;

        if (scheduled) {
            video.setPublicationState(Video.PublicationState.SCHEDULED);
            video.setPublishedAt(request.getScheduledAt());
        } else {
            video.publish();
        }

        videoRepository.save(video);

        if (!scheduled) {
            publishVideoPublishedEvent(video);
        }

        boolean publicationStateChanged =
                wasPublished != (video.getPublicationState() == Video.PublicationState.PUBLISHED);

        if (publicationStateChanged) {
            videoVersionService.createVideoVersion(
                    videoId, "Published video", userId);
        }

        log.info("Processed publish request for video: {}", videoId);
        return mapToVideoResponse(video, null, null);
    }

    @Transactional
    public VideoResponse unpublishVideo(UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.isDeleted()) {
            throw new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found");
        }

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to unpublish this video");
        }

        boolean wasPublished =
                video.getPublicationState() == Video.PublicationState.PUBLISHED;

        video.unpublish();
        videoRepository.save(video);
        publishVideoUnpublishedEvent(video);

        if (wasPublished) {
            videoVersionService.createVideoVersion(
                    videoId, "Unpublished video", userId);
        }

        log.info("Unpublished video: {}", videoId);
        return mapToVideoResponse(video, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoListProjection> getMyVideos(int page, int size) {
        validatePageRequest(page, size);

        UUID userId = currentUserAccessor.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoListProjection> videoPage =
                videoRepository.findByCreatorIdProjected(userId, pageable);

        return PageResponse.of(
                videoPage.getContent(), page, size, videoPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoListProjection> getChannelVideos(
            UUID channelId, int page, int size) {

        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoListProjection> videoPage =
                videoRepository.findByChannelIdProjected(channelId, pageable);

        return PageResponse.of(
                videoPage.getContent(), page, size, videoPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoListProjection> getPublishedVideos(int page, int size) {
        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoListProjection> videoPage =
                videoRepository.findAllProjected(pageable);

        return PageResponse.of(
                videoPage.getContent(), page, size, videoPage.getTotalElements());
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BusinessException(
                    ApiErrorCodes.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "Page index cannot be negative");
        }

        if (size < 1 || size > 200) {
            throw new BusinessException(
                    ApiErrorCodes.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 200");
        }
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

    @Transactional
    public void saveVideoFile(UUID videoId, VideoFileInfo fileInfo) {
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

        if (fileInfo.getFileType() == FileType.DUBBED_AUDIO
                && fileInfo.getResolution() != null) {
            try {
                languageRepository.findByCode(fileInfo.getResolution())
                        .ifPresent(language -> videoFile.setLanguageId(language.getId()));
            } catch (Exception e) {
                log.warn("Could not find language for code {}: {}",
                        fileInfo.getResolution(), e.getMessage());
            }
        }

        videoFileRepository.save(videoFile);
    }

    @Transactional(readOnly = true)
    public String generatePresignedHlsUrl(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (video.getHlsManifestKey() == null) {
            throw new BusinessException(
                    ApiErrorCodes.VIDEO_PROCESSING,
                    HttpStatus.CONFLICT,
                    "HLS manifest not available");
        }

        return storageProvider.generatePresignedGetUrl(
                "hls", video.getHlsManifestKey(), 4 * 60 * 60);
    }

    public String generatePresignedSegmentUrl(String objectKey) {
        return storageProvider.generatePresignedGetUrl(
                "hls", objectKey, 4 * 60 * 60);
    }

    private void checkVideoAccess(Video video) {
        UUID currentUserId = currentUserAccessor.getCurrentUserId();

        if (video.isPublished()) {
            return;
        }

        if (currentUserId != null && video.getCreatorId().equals(currentUserId)) {
            return;
        }

        if (currentUserAccessor.isAdmin()) {
            return;
        }

        if (video.getVisibility() == Video.Visibility.UNLISTED) {
            return;
        }

        throw new BusinessException(
                ApiErrorCodes.VIDEO_PRIVATE,
                HttpStatus.FORBIDDEN,
                "Video is private");
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
                .genreName(video.getGenreId() != null
                        ? genreRepository.findById(video.getGenreId())
                        .map(Genre::getName).orElse(null)
                        : null)
                .categoryId(video.getCategoryId())
                .categoryName(video.getCategoryId() != null
                        ? categoryRepository.findById(video.getCategoryId())
                        .map(Category::getName).orElse(null)
                        : null)
                .languageId(video.getLanguageId())
                .languageName(video.getLanguageId() != null
                        ? languageRepository.findById(video.getLanguageId())
                        .map(Language::getName).orElse(null)
                        : null)
                .durationSeconds(video.getDurationSeconds())
                .thumbnailUrl(video.getThumbnailUrl())
                .visibility(video.getVisibility().name())
                .availableResolutions(video.getAvailableResolutions())
                .publishedAt(video.getPublishedAt())
                .channelName(channel != null ? channel.getName() : null)
                .channelSlug(channel != null ? channel.getSlug() : null)
                .build();

        EventEnvelope<VideoPublishedEvent> envelope = EventEnvelope.create(
                "video.published", "v1", event, "video-service");

        kafkaTemplate.send(
                KafkaTopicNames.VIDEO_PUBLISHED, video.getId().toString(), envelope);
    }

    private void publishVideoUnpublishedEvent(Video video) {
        VideoUnpublishedEvent event = VideoUnpublishedEvent.builder()
                .videoId(video.getId())
                .reason("Unpublished by creator")
                .unpublishedAt(Instant.now())
                .build();

        EventEnvelope<VideoUnpublishedEvent> envelope = EventEnvelope.create(
                "video.unpublished", "v1", event, "video-service");

        kafkaTemplate.send(
                KafkaTopicNames.VIDEO_UNPUBLISHED, video.getId().toString(), envelope);
    }

    private void publishVideoDeletedEvent(Video video, UUID deletedBy) {
        VideoDeletedEvent event = VideoDeletedEvent.builder()
                .videoId(video.getId())
                .deletedBy(deletedBy)
                .deletedAt(Instant.now())
                .build();

        EventEnvelope<VideoDeletedEvent> envelope = EventEnvelope.create(
                "video.deleted", "v1", event, "video-service");

        kafkaTemplate.send(
                KafkaTopicNames.VIDEO_DELETED, video.getId().toString(), envelope);
    }

    public VideoResponse mapAdminResponse(Video video) {
        return mapToVideoResponse(video, null, null);
    }

    private VideoResponse mapToVideoResponse(
            Video video,
            com.platform.user.entity.CreatorProfile creatorProfile,
            Channel channel) {

        return VideoResponse.builder()
                .id(video.getId())
                .creatorId(video.getCreatorId())
                .channelId(video.getChannelId())
                .title(video.getTitle())
                .slug(video.getSlug())
                .description(video.getDescription())
                .tags(video.getTags())
                .genreId(video.getGenreId())
                .genreName(video.getGenreId() != null
                        ? genreRepository.findById(video.getGenreId())
                        .map(Genre::getName).orElse(null)
                        : null)
                .categoryId(video.getCategoryId())
                .categoryName(video.getCategoryId() != null
                        ? categoryRepository.findById(video.getCategoryId())
                        .map(Category::getName).orElse(null)
                        : null)
                .languageId(video.getLanguageId())
                .languageName(video.getLanguageId() != null
                        ? languageRepository.findById(video.getLanguageId())
                        .map(Language::getName).orElse(null)
                        : null)
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
                .hlsManifestUrl(video.getHlsManifestKey() != null
                        ? generatePresignedHlsUrl(video.getId())
                        : null)
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .creatorName(creatorProfile != null
                        ? creatorProfile.getChannelName()
                        : null)
                .channelName(channel != null ? channel.getName() : null)
                .channelSlug(channel != null ? channel.getSlug() : null)
                .build();
    }

    private Set<String> extractHashtags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty() && tag.startsWith("#"))
                .collect(Collectors.toSet());
    }

    private Set<String> extractMentions(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        return Pattern.compile("@([a-zA-Z0-9_]+)")
                .matcher(text)
                .results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse mapToVideoDetailResponse(Video video) {
        List<VideoFile> files = videoFileRepository.findByVideoId(video.getId());

        List<VideoResolutionInfo> resolutionDetails = files.stream()
                .filter(file -> file.getFileType() == FileType.HLS_MANIFEST
                        && !file.isMaster())
                .map(file -> VideoResolutionInfo.builder()
                        .resolution(file.getResolution())
                        .width(file.getWidth() != null
                                ? file.getWidth().toString() : null)
                        .height(file.getHeight() != null
                                ? file.getHeight().toString() : null)
                        .bitrate(file.getBitrate() != null
                                ? file.getBitrate().longValue() : null)
                        .codec(file.getCodec())
                        .manifestUrl(storageProvider.generatePresignedGetUrl(
                                file.getBucket(), file.getObjectKey(), 4 * 60 * 60))
                        .build())
                .toList();

        Set<String> hashtags = extractHashtags(video.getTags());
        Set<String> mentions = extractMentions(video.getDescription());

        return VideoDetailResponse.builder()
                .id(video.getId())
                .creatorId(video.getCreatorId())
                .channelId(video.getChannelId())
                .title(video.getTitle())
                .slug(video.getSlug())
                .description(video.getDescription())
                .tags(video.getTags())
                .genreId(video.getGenreId())
                .genreName(video.getGenreId() != null
                        ? genreRepository.findById(video.getGenreId())
                        .map(Genre::getName).orElse(null)
                        : null)
                .categoryId(video.getCategoryId())
                .categoryName(video.getCategoryId() != null
                        ? categoryRepository.findById(video.getCategoryId())
                        .map(Category::getName).orElse(null)
                        : null)
                .languageId(video.getLanguageId())
                .languageName(video.getLanguageId() != null
                        ? languageRepository.findById(video.getLanguageId())
                        .map(Language::getName).orElse(null)
                        : null)
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
                .hlsManifestUrl(video.getHlsManifestKey() != null
                        ? generatePresignedHlsUrl(video.getId())
                        : null)
                .resolutionDetails(resolutionDetails)
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .creatorName(null)
                .channelName(null)
                .channelSlug(null)
                .channelAvatarUrl(null)
                .isSubscribed(false)
                .isLiked(false)
                .watchProgress(null)
                .hashtags(hashtags)
                .mentions(mentions)
                .build();
    }

    /**
     * Perform bulk operations on videos.
     *
     * Versioning for PUBLISH and UNPUBLISH is handled by the corresponding
     * methods. UPDATE_TAGS and UPDATE_VISIBILITY create their version here.
     */
    @Transactional
    public BulkVideoOperationResponse bulkVideoOperation(
            BulkVideoOperationRequest request) {

        if (request == null || request.getOperation() == null
                || request.getOperation().isBlank()) {
            throw new BusinessException(
                    ApiErrorCodes.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "Bulk operation is required");
        }

        if (request.getVideoIds() == null || request.getVideoIds().isEmpty()) {
            throw new BusinessException(
                    ApiErrorCodes.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "At least one video ID is required");
        }

        UUID userId = currentUserAccessor.getCurrentUserId();

        int successfulCount = 0;
        int failedCount = 0;

        List<UUID> successfulVideoIds = new ArrayList<>();
        List<UUID> failedVideoIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        String operation = request.getOperation().trim().toUpperCase();

        for (UUID videoId : request.getVideoIds()) {
            if (videoId == null) {
                failedCount++;
                errorMessages.add("Video ID cannot be null");
                continue;
            }

            try {
                Video video = videoRepository.findById(videoId)
                        .orElseThrow(() -> new NotFoundException(
                                ApiErrorCodes.VIDEO_NOT_FOUND,
                                "Video not found: " + videoId));

                if (!video.getCreatorId().equals(userId)
                        && !currentUserAccessor.isAdmin()) {
                    throw new BusinessException(
                            ApiErrorCodes.ACCESS_DENIED,
                            HttpStatus.FORBIDDEN,
                            "Not authorized to perform operation on video: " + videoId);
                }

                boolean createVersion = false;
                String changeSummary = null;

                switch (operation) {
                    case "PUBLISH" -> {
                        // publishVideo handles publication versioning.
                        publishVideo(videoId, new PublishRequest(false, null));
                    }
                    case "UNPUBLISH" -> {
                        // unpublishVideo handles publication versioning.
                        unpublishVideo(videoId);
                    }
                    case "DELETE" -> deleteVideo(videoId);
                    case "RESTORE" -> restoreVideo(videoId, userId);
                    case "UPDATE_TAGS" -> {
                        if (request.getTags() == null) {
                            throw new BusinessException(
                                    ApiErrorCodes.INVALID_REQUEST,
                                    HttpStatus.BAD_REQUEST,
                                    "Tags parameter is required for UPDATE_TAGS");
                        }

                        updateVideoTags(videoId, request.getTags());
                        createVersion = true;
                        changeSummary = "Updated tags via bulk operation";
                    }
                    case "UPDATE_VISIBILITY" -> {
                        if (request.getVisibility() == null) {
                            throw new BusinessException(
                                    ApiErrorCodes.INVALID_REQUEST,
                                    HttpStatus.BAD_REQUEST,
                                    "Visibility parameter is required for UPDATE_VISIBILITY");
                        }

                        // Validate before mutating the video.
                        Video.Visibility.valueOf(request.getVisibility());

                        updateVideoVisibility(videoId, request.getVisibility());
                        createVersion = true;
                        changeSummary = "Updated visibility via bulk operation";
                    }
                    default -> throw new BusinessException(
                            ApiErrorCodes.INVALID_REQUEST,
                            HttpStatus.BAD_REQUEST,
                            "Unsupported operation: " + operation);
                }

                if (createVersion) {
                    videoVersionService.createVideoVersion(
                            videoId, changeSummary, userId);
                }

                successfulCount++;
                successfulVideoIds.add(videoId);

            } catch (Exception e) {
                failedCount++;
                failedVideoIds.add(videoId);

                String message = e.getMessage() != null
                        ? e.getMessage()
                        : e.getClass().getSimpleName();

                errorMessages.add("Video " + videoId + ": " + message);

                log.error(
                        "Failed bulk operation {} for video {}",
                        operation, videoId, e);
            }
        }

        return BulkVideoOperationResponse.builder()
                .successfulCount(successfulCount)
                .failedCount(failedCount)
                .successfulVideoIds(successfulVideoIds)
                .failedVideoIds(failedVideoIds)
                .errorMessages(errorMessages)
                .build();
    }

    @Transactional
    public void restoreVideo(UUID videoId, UUID restoredBy) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND,
                        "Video not found: " + videoId));

        if (!video.isDeleted()) {
            throw new BusinessException(
                    ApiErrorCodes.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "Video is not deleted: " + videoId);
        }

        if (!video.getCreatorId().equals(restoredBy)
                && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to restore video: " + videoId);
        }

        video.restore();
        videoRepository.save(video);

        log.info("Restored video: {}", videoId);
    }

    @Transactional
    public void updateVideoTags(UUID videoId, String tags) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND,
                        "Video not found: " + videoId));

        UUID userId = currentUserAccessor.getCurrentUserId();

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to update video: " + videoId);
        }

        video.setTags(tags);
        videoRepository.save(video);

        log.info("Updated tags for video: {}", videoId);
    }

    @Transactional
    public void updateVideoVisibility(UUID videoId, String visibility) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND,
                        "Video not found: " + videoId));

        UUID userId = currentUserAccessor.getCurrentUserId();

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to update video: " + videoId);
        }

        Video.Visibility newVisibility = Video.Visibility.valueOf(visibility);
        video.setVisibility(newVisibility);
        videoRepository.save(video);

        log.info("Updated visibility for video: {}", videoId);
    }

    @Transactional(readOnly = true)
    public List<Video> getDeletedVideos(UUID userId) {
        return videoRepository.findDeletedVideosByCreatorId(userId);
    }

    @Transactional(readOnly = true)
    public Page<Video> getDeletedVideos(UUID userId, int page, int size) {
        validatePageRequest(page, size);

        List<Video> deletedVideos = getDeletedVideos(userId);

        int start = Math.min(page * size, deletedVideos.size());
        int end = Math.min(start + size, deletedVideos.size());

        List<Video> pagedVideos = deletedVideos.subList(start, end);

        return new PageImpl<>(
                pagedVideos,
                PageRequest.of(page, size),
                deletedVideos.size());
    }

    @Transactional(readOnly = true)
    public List<VideoFileInfo> getVideoCaptions(UUID videoId) {
        return videoFileRepository.findByVideoIdAndFileTypeIn(
                        videoId, List.of(FileType.VTT_CAPTION, FileType.SRT_CAPTION))
                .stream()
                .map(this::toVideoFileInfo)
                .toList();
    }

    @Transactional
    public void addVideoCaptions(UUID videoId, List<VideoFileInfo> captionFiles) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND,
                        "Video not found: " + videoId));

        UUID userId = currentUserAccessor.getCurrentUserId();

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to modify video: " + videoId);
        }

        if (captionFiles == null || captionFiles.isEmpty()) {
            return;
        }

        for (VideoFileInfo captionFile : captionFiles) {
            if (captionFile.getFileType() != FileType.VTT_CAPTION
                    && captionFile.getFileType() != FileType.SRT_CAPTION) {
                throw new BusinessException(
                        ApiErrorCodes.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST,
                        "Only VTT_CAPTION and SRT_CAPTION files are allowed");
            }

            VideoFile videoFile = new VideoFile();
            videoFile.setVideoId(videoId);
            videoFile.setFileType(captionFile.getFileType());
            videoFile.setResolution(captionFile.getResolution());
            videoFile.setObjectKey(captionFile.getObjectKey());
            videoFile.setBucket(captionFile.getBucket());
            videoFile.setFileSize(captionFile.getFileSize());
            videoFile.setContentType(captionFile.getContentType());
            videoFile.setChecksum(captionFile.getChecksum());
            videoFile.setDurationSeconds(captionFile.getDurationSeconds());
            videoFile.setBitrate(captionFile.getBitrate());
            videoFile.setWidth(captionFile.getWidth());
            videoFile.setHeight(captionFile.getHeight());
            videoFile.setCodec(captionFile.getCodec());
            videoFile.setSegmentIndex(captionFile.getSegmentIndex());
            videoFile.setMaster(captionFile.isMaster());

            videoFileRepository.save(videoFile);
        }

        log.info("Added {} caption files to video {}", captionFiles.size(), videoId);
    }

    @Transactional
    public void deleteVideoCaptions(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND,
                        "Video not found: " + videoId));

        UUID userId = currentUserAccessor.getCurrentUserId();

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to modify video: " + videoId);
        }

        List<VideoFile> captionFiles =
                videoFileRepository.findByVideoIdAndFileTypeIn(
                        videoId, List.of(FileType.VTT_CAPTION, FileType.SRT_CAPTION));

        videoFileRepository.deleteAll(captionFiles);

        log.info("Deleted {} caption files from video {}", captionFiles.size(), videoId);
    }

    @Transactional(readOnly = true)
    public List<VideoFileInfo> getVideoDubbedAudio(UUID videoId) {
        return videoFileRepository.findByVideoIdAndFileType(
                        videoId, FileType.DUBBED_AUDIO)
                .stream()
                .map(this::toVideoFileInfo)
                .toList();
    }

    @Transactional
    public void addVideoDubbedAudio(
            UUID videoId, List<VideoFileInfo> dubbedAudioFiles) {

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND,
                        "Video not found: " + videoId));

        UUID userId = currentUserAccessor.getCurrentUserId();

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to modify video: " + videoId);
        }

        if (dubbedAudioFiles == null || dubbedAudioFiles.isEmpty()) {
            return;
        }

        for (VideoFileInfo dubbedAudioFile : dubbedAudioFiles) {
            if (dubbedAudioFile.getFileType() != FileType.DUBBED_AUDIO) {
                throw new BusinessException(
                        ApiErrorCodes.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST,
                        "Only DUBBED_AUDIO files are allowed");
            }

            VideoFile videoFile = new VideoFile();
            videoFile.setVideoId(videoId);
            videoFile.setFileType(dubbedAudioFile.getFileType());
            videoFile.setResolution(dubbedAudioFile.getResolution());
            videoFile.setObjectKey(dubbedAudioFile.getObjectKey());
            videoFile.setBucket(dubbedAudioFile.getBucket());
            videoFile.setFileSize(dubbedAudioFile.getFileSize());
            videoFile.setContentType(dubbedAudioFile.getContentType());
            videoFile.setChecksum(dubbedAudioFile.getChecksum());
            videoFile.setDurationSeconds(dubbedAudioFile.getDurationSeconds());
            videoFile.setBitrate(dubbedAudioFile.getBitrate());
            videoFile.setWidth(dubbedAudioFile.getWidth());
            videoFile.setHeight(dubbedAudioFile.getHeight());
            videoFile.setCodec(dubbedAudioFile.getCodec());
            videoFile.setSegmentIndex(dubbedAudioFile.getSegmentIndex());
            videoFile.setMaster(dubbedAudioFile.isMaster());

            if (dubbedAudioFile.getResolution() != null) {
                try {
                    languageRepository.findByCode(dubbedAudioFile.getResolution())
                            .ifPresent(language ->
                                    videoFile.setLanguageId(language.getId()));
                } catch (Exception e) {
                    log.warn("Could not find language for code {}: {}",
                            dubbedAudioFile.getResolution(), e.getMessage());
                }
            }

            videoFileRepository.save(videoFile);
        }

        log.info("Added {} dubbed audio files to video {}",
                dubbedAudioFiles.size(), videoId);
    }

    @Transactional
    public void deleteVideoDubbedAudio(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.VIDEO_NOT_FOUND,
                        "Video not found: " + videoId));

        UUID userId = currentUserAccessor.getCurrentUserId();

        if (!video.getCreatorId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.FORBIDDEN,
                    "Not authorized to modify video: " + videoId);
        }

        List<VideoFile> dubbedAudioFiles =
                videoFileRepository.findByVideoIdAndFileType(
                        videoId, FileType.DUBBED_AUDIO);

        videoFileRepository.deleteAll(dubbedAudioFiles);

        log.info("Deleted {} dubbed audio files from video {}",
                dubbedAudioFiles.size(), videoId);
    }

    private VideoFileInfo toVideoFileInfo(VideoFile file) {
        return VideoFileInfo.builder()
                .videoId(file.getVideoId())
                .fileType(file.getFileType())
                .resolution(file.getResolution())
                .objectKey(file.getObjectKey())
                .bucket(file.getBucket())
                .fileSize(file.getFileSize())
                .contentType(file.getContentType())
                .checksum(file.getChecksum())
                .durationSeconds(file.getDurationSeconds())
                .bitrate(file.getBitrate())
                .width(file.getWidth())
                .height(file.getHeight())
                .codec(file.getCodec())
                .segmentIndex(file.getSegmentIndex())
                .master(file.isMaster())
                .build();
    }

    private String buildChangeSummary(UpdateVideoRequest request) {
        List<String> changedFields = new ArrayList<>();

        if (request.getTitle() != null) {
            changedFields.add("title");
        }
        if (request.getDescription() != null) {
            changedFields.add("description");
        }
        if (request.getTags() != null) {
            changedFields.add("tags");
        }
        if (request.getVisibility() != null) {
            changedFields.add("visibility");
        }
        if (request.getAllowComments() != null) {
            changedFields.add("allowComments");
        }
        if (request.getAllowEmbedding() != null) {
            changedFields.add("allowEmbedding");
        }
        if (request.getAgeRestricted() != null) {
            changedFields.add("ageRestricted");
        }
        if (request.getThumbnailUrl() != null) {
            changedFields.add("thumbnailUrl");
        }
        if (request.getPreviewUrl() != null) {
            changedFields.add("previewUrl");
        }
        if (request.getCategoryId() != null) {
            changedFields.add("category");
        }
        if (request.getGenreId() != null) {
            changedFields.add("genre");
        }
        if (request.getLanguageId() != null) {
            changedFields.add("language");
        }

        return changedFields.isEmpty()
                ? "No changes"
                : "Updated fields: " + String.join(", ", changedFields);
    }
}
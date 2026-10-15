package com.platform.video.service;

import com.platform.video.dto.VideoVersionDTO;
import com.platform.common.entity.Video;
import com.platform.video.entity.VideoVersion;
import com.platform.video.repository.VideoRepository;
import com.platform.video.repository.VideoVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling video versioning (Feature 21: Video Versioning)
 */
@Service
@RequiredArgsConstructor
public class VideoVersionService {

    private final VideoVersionRepository videoVersionRepository;
    private final VideoRepository videoRepository;

    /**
     * Create a new version of a video
     */
    @Transactional
    public VideoVersion createVideoVersion(UUID videoId, String changeSummary, UUID changedBy) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found with id: " + videoId));

        // Get the current version number for this video
        Integer currentVersionNumber = videoVersionRepository.findByVideoIdOrderByVersionNumberDesc(videoId)
                .stream()
                .findFirst()
                .map(VideoVersion::getVersionNumber)
                .orElse(0);

        // Create new version
        VideoVersion version = new VideoVersion();
        version.setVideoId(videoId);
        version.setVersionNumber(currentVersionNumber + 1);
        version.setTitle(video.getTitle());
        version.setDescription(video.getDescription());
        version.setTags(video.getTags());
        version.setCategoryId(video.getCategoryId());
        version.setGenreId(video.getGenreId());
        version.setLanguageId(video.getLanguageId());
        version.setVisibility(video.getVisibility().name());
        version.setProcessingState(video.getProcessingState().name());
        version.setPublicationState(video.getPublicationState().name());
        version.setPublishedAt(video.getPublishedAt());
        version.setDurationSeconds(video.getDurationSeconds());
        version.setThumbnailUrl(video.getThumbnailUrl());
        version.setPreviewUrl(video.getPreviewUrl());
        version.setAgeRestricted(video.isAgeRestricted());
        version.setAllowComments(video.isAllowComments());
        version.setAllowEmbedding(video.isAllowEmbedding());
        version.setChangeSummary(changeSummary);
        version.setChangedBy(changedBy);
        version.setCreatedAt(Instant.now());
        version.setUpdatedAt(Instant.now());

        // Mark all previous versions as not current
        List<VideoVersion> existingVersions = videoVersionRepository.findByVideoId(videoId);
        for (VideoVersion existingVersion : existingVersions) {
            existingVersion.setCurrent(false);
        }
        videoVersionRepository.saveAll(existingVersions);

        // Set new version as current
        version.setCurrent(true);
        VideoVersion savedVersion = videoVersionRepository.save(version);

        return savedVersion;
    }

    /**
     * Get a video version by ID
     */
    public VideoVersion getVideoVersionById(UUID id) {
        return videoVersionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video version not found with id: " + id));
    }

    /**
     * Get all versions of a video
     */
    public List<VideoVersion> getVideoVersions(UUID videoId) {
        return videoVersionRepository.findByVideoIdOrderByVersionNumberDesc(videoId);
    }

    /**
     * Get the current version of a video
     */
    public VideoVersion getCurrentVideoVersion(UUID videoId) {
        return videoVersionRepository.findByVideoIdAndIsCurrentTrue(videoId);
    }

    /**
     * Get video versions with filtering and pagination
     */
    public Page<VideoVersion> getVideoVersionsByCriteria(
            UUID videoId,
            Integer versionNumber,
            String title,
            UUID changedBy,
            Boolean isCurrent,
            Instant startTime,
            Instant endTime,
            Pageable pageable) {
        return videoVersionRepository.findByCriteria(
                videoId, versionNumber, title, changedBy, isCurrent, startTime, endTime, pageable);
    }

    /**
     * Convert VideoVersion entity to VideoVersionDTO
     */
    public VideoVersionDTO mapToDTO(VideoVersion version) {
        return VideoVersionDTO.builder()
                .id(version.getId())
                .videoId(version.getVideoId())
                .versionNumber(version.getVersionNumber())
                .title(version.getTitle())
                .description(version.getDescription())
                .tags(version.getTags())
                .categoryId(version.getCategoryId())
                .genreId(version.getGenreId())
                .languageId(version.getLanguageId())
                .visibility(version.getVisibility())
                .processingState(version.getProcessingState())
                .publicationState(version.getPublicationState())
                .publishedAt(version.getPublishedAt())
                .durationSeconds(version.getDurationSeconds())
                .thumbnailUrl(version.getThumbnailUrl())
                .previewUrl(version.getPreviewUrl())
                .ageRestricted(version.isAgeRestricted())
                .allowComments(version.isAllowComments())
                .allowEmbedding(version.isAllowEmbedding())
                .changeSummary(version.getChangeSummary())
                .changedBy(version.getChangedBy())
                .isCurrent(version.isCurrent())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .build();
    }

    /**
     * Convert list of VideoVersion entities to list of VideoVersionDTOs
     */
    public List<VideoVersionDTO> mapToDTOList(List<VideoVersion> versions) {
        return versions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
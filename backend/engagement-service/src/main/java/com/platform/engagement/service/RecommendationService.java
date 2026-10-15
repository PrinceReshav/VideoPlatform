
package com.platform.engagement.service;

import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.engagement.entity.Like;
import com.platform.engagement.entity.WatchHistory;
import com.platform.engagement.repository.LikeRepository;
import com.platform.engagement.repository.ViewRepository;
import com.platform.engagement.repository.WatchHistoryRepository;
import com.platform.video.dto.VideoResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating video recommendations
 * (Feature 28: Recommendation Engine).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final LikeRepository likeRepository;
    private final ViewRepository viewRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoRepository videoRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Gets recommended videos for the current user based on engagement history.
     */
    @Transactional(readOnly = true)
    public List<VideoResponse> getRecommendedVideos(int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 100));

        UUID userId = currentUserAccessor.getCurrentUserId();

        // Get videos liked by the current user.
        List<Like> userLikes = likeRepository.findByUserId(userId);

        Set<UUID> likedVideoIds = userLikes.stream()
                .map(Like::getVideoId)
                .filter(videoId -> videoId != null)
                .collect(Collectors.toSet());

        // Get the user's watch history.
        List<WatchHistory> userWatchHistory =
                watchHistoryRepository.findByUserId(userId);

        // Consider completed videos or videos watched to at least 50%.
        Set<UUID> watchedVideoIds = userWatchHistory.stream()
                .filter(history ->
                        history.isCompleted()
                                || history.getProgressPercent() >= 50)
                .map(WatchHistory::getVideoId)
                .filter(videoId -> videoId != null)
                .collect(Collectors.toSet());

        // Combine liked and significantly watched videos.
        Set<UUID> userInterestedVideoIds = new HashSet<>();
        userInterestedVideoIds.addAll(likedVideoIds);
        userInterestedVideoIds.addAll(watchedVideoIds);

        // If the user has no engagement history, recommend popular videos.
        if (userInterestedVideoIds.isEmpty()) {
            return getPopularVideos(safeLimit);
        }

        // Load the videos that indicate the user's interests.
        List<Video> interestedVideos =
                videoRepository.findAllById(userInterestedVideoIds);

        // Extract preferred categories.
        Set<UUID> userPreferredCategories = interestedVideos.stream()
                .map(Video::getCategoryId)
                .filter(categoryId -> categoryId != null)
                .collect(Collectors.toSet());

        // Extract preferred genres.
        Set<UUID> userPreferredGenres = interestedVideos.stream()
                .map(Video::getGenreId)
                .filter(genreId -> genreId != null)
                .collect(Collectors.toSet());

        // If no category or genre information is available,
        // avoid executing a recommendation query with empty IN parameters.
        if (userPreferredCategories.isEmpty()
                && userPreferredGenres.isEmpty()) {
            return getPopularVideos(safeLimit);
        }

        // Find public videos matching the user's interests,
        // excluding videos they have already liked or significantly watched.
        List<Video> recommendedVideos =
                videoRepository.findRecommendedVideos(
                        userPreferredCategories,
                        userPreferredGenres,
                        userInterestedVideoIds,
                        PageRequest.of(0, safeLimit)
                );

        // If no matching recommendations exist, show popular videos.
        if (recommendedVideos.isEmpty()) {
            return getPopularVideos(safeLimit);
        }

        return recommendedVideos.stream()
                .map(this::mapVideoResponseSafely)
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * Gets popular videos as a fallback.
     */
    @Transactional(readOnly = true)
    public List<VideoResponse> getPopularVideos(int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 100));

        List<Video> popularVideos = videoRepository.findPopularVideos(
                PageRequest.of(0, safeLimit)
        );

        return popularVideos.stream()
                .map(this::mapVideoResponseSafely)
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * Maps the fields available in the shared Video entity to the response DTO.
     *
     * Engagement Service deliberately does not inject VideoService: that class
     * belongs to a different independently-running service and has many
     * unrelated infrastructure dependencies. Fields that require Video
     * Service-only lookups or storage signing (genre/category/language names,
     * HLS presigned URL, creator/channel display data) are left null here.
     */
    private VideoResponse mapVideoResponseSafely(Video video) {
        if (video == null) {
            return null;
        }

        try {
            return VideoResponse.builder()
                    .id(video.getId())
                    .creatorId(video.getCreatorId())
                    .channelId(video.getChannelId())
                    .title(video.getTitle())
                    .slug(video.getSlug())
                    .description(video.getDescription())
                    .tags(video.getTags())
                    .genreId(video.getGenreId())
                    .categoryId(video.getCategoryId())
                    .languageId(video.getLanguageId())
                    .durationSeconds(video.getDurationSeconds())
                    .thumbnailUrl(video.getThumbnailUrl())
                    .previewUrl(video.getPreviewUrl())
                    .visibility(video.getVisibility() != null
                            ? video.getVisibility().name() : null)
                    .processingState(video.getProcessingState() != null
                            ? video.getProcessingState().name() : null)
                    .publicationState(video.getPublicationState() != null
                            ? video.getPublicationState().name() : null)
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
                    .createdAt(video.getCreatedAt())
                    .updatedAt(video.getUpdatedAt())
                    .build();
        } catch (RuntimeException e) {
            log.warn("Failed to map video {} to response", video.getId(), e);
            return null;
        }
    }
}

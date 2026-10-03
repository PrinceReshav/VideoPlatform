package com.platform.engagement.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.engagement.dto.CommentRequest;
import com.platform.engagement.dto.CommentResponse;
import com.platform.engagement.dto.LikeRequest;
import com.platform.engagement.dto.LikeResponse;
import com.platform.engagement.dto.UpdateCommentRequest;
import com.platform.engagement.dto.WatchHistoryResponse;
import com.platform.engagement.entity.Comment;
import com.platform.engagement.entity.Like;
import com.platform.engagement.entity.View;
import com.platform.engagement.entity.WatchHistory;
import com.platform.common.event.CommentCreatedEvent;
import com.platform.common.event.VideoLikedEvent;
import com.platform.common.event.VideoViewedEvent;
import com.platform.engagement.repository.CommentRepository;
import com.platform.engagement.repository.LikeRepository;
import com.platform.engagement.repository.ViewRepository;
import com.platform.engagement.repository.WatchHistoryRepository;
import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngagementService {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ViewRepository viewRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoRepository videoRepository;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional
    public LikeResponse toggleLike(LikeRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        UUID videoId = request.getVideoId();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        var existingLike = likeRepository.findByUserIdAndVideoId(userId, videoId);

        if (existingLike.isPresent()) {
            // Unlike
            likeRepository.deleteByUserIdAndVideoId(userId, videoId);
            videoRepository.decrementLikeCount(videoId);

            publishVideoLikedEvent(videoId, userId, false);
            log.debug("User {} unliked video {}", userId, videoId);
            return LikeResponse.builder().liked(false).likeCount(likeRepository.countByVideoId(videoId)).build();
        } else {
            // Like
            Like like = new Like();
            like.setUserId(userId);
            like.setVideoId(videoId);
            likeRepository.save(like);
            videoRepository.incrementLikeCount(videoId);

            publishVideoLikedEvent(videoId, userId, true);
            log.debug("User {} liked video {}", userId, videoId);
            return LikeResponse.builder().liked(true).likeCount(likeRepository.countByVideoId(videoId)).build();
        }
    }

    @Transactional(readOnly = true)
    public boolean isLiked(UUID videoId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        return likeRepository.existsByUserIdAndVideoId(userId, videoId);
    }

    @Transactional(readOnly = true)
    public long getLikeCount(UUID videoId) {
        return likeRepository.countByVideoId(videoId);
    }

    @Transactional
    public CommentResponse addComment(CommentRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        UUID videoId = request.getVideoId();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.VIDEO_NOT_FOUND, "Video not found"));

        if (!video.isAllowComments()) {
            throw new BusinessException(ApiErrorCodes.COMMENT_EMPTY,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Comments are disabled for this video");
        }

        Comment comment = new Comment();
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);

        // Update reply count on parent
        if (request.getParentId() != null) {
            commentRepository.findById(request.getParentId()).ifPresent(parent -> {
                parent.setReplyCount(parent.getReplyCount() + 1);
                commentRepository.save(parent);
            });
        }

        videoRepository.incrementCommentCount(videoId);

        // Publish event
        publishCommentCreatedEvent(comment);

        log.debug("User {} commented on video {}", userId, videoId);
        return mapToCommentResponse(comment, userId);
    }

    @Transactional
    public CommentResponse updateComment(UUID commentId, UpdateCommentRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.COMMENT_NOT_FOUND, "Comment not found"));

        if (!comment.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to edit this comment");
        }

        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);

        log.debug("User {} updated comment {}", userId, commentId);
        return mapToCommentResponse(comment, userId);
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.COMMENT_NOT_FOUND, "Comment not found"));

        if (!comment.getUserId().equals(userId) && !currentUserAccessor.isAdmin()) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized to delete this comment");
        }

        // If it has a parent, decrement reply count
        if (comment.getParentId() != null) {
            commentRepository.findById(comment.getParentId()).ifPresent(parent -> {
                parent.setReplyCount(Math.max(0, parent.getReplyCount() - 1));
                commentRepository.save(parent);
            });
        }

        // Soft delete - mark as hidden
        comment.setHidden(true);
        comment.setHiddenBy(userId);
        comment.setHiddenAt(Instant.now());
        comment.setHiddenReason("Deleted by user");
        commentRepository.save(comment);

        videoRepository.decrementCommentCount(comment.getVideoId());

        log.debug("User {} deleted comment {}", userId, commentId);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(UUID videoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByVideoIdAndHiddenFalse(videoId, pageable);

        return PageResponse.of(
                comments.getContent().stream()
                        .map(c -> mapToCommentResponse(c, currentUserAccessor.getCurrentUserId()))
                        .toList(),
                page, size, comments.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentThread(UUID videoId) {
        List<Comment> rootComments = commentRepository.findByVideoIdAndParentIdIsNullAndHiddenFalseOrderByCreatedAtDesc(videoId);
        UUID currentUserId = currentUserAccessor.getCurrentUserId();

        return rootComments.stream()
                .map(c -> mapToCommentResponseWithReplies(c, currentUserId))
                .toList();
    }

    @Transactional
    public void recordView(UUID videoId, Integer positionSeconds, Integer durationSeconds, String sessionId) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        // Record view (async, don't block)
        View view = new View();
        view.setVideoId(videoId);
        view.setUserId(userId);
        view.setSessionId(sessionId);
        view.setPositionSeconds(positionSeconds);
        view.setDurationSeconds(durationSeconds);
        view.setCompleted(positionSeconds != null && durationSeconds != null && positionSeconds >= durationSeconds * 0.9);
        viewRepository.save(view);

        // Update watch history
        updateWatchHistory(userId, videoId, positionSeconds, durationSeconds);

        // Increment view count on video (async via event)
        publishVideoViewedEvent(videoId, userId, positionSeconds, durationSeconds);
    }

    @Transactional
    public void updateWatchHistory(UUID userId, UUID videoId, Integer positionSeconds, Integer durationSeconds) {
        var existing = watchHistoryRepository.findByUserIdAndVideoId(userId, videoId);

        WatchHistory history;
        if (existing.isPresent()) {
            history = existing.get();
            history.setPositionSeconds(positionSeconds != null ? positionSeconds : history.getPositionSeconds());
            history.setDurationSeconds(durationSeconds != null ? durationSeconds : history.getDurationSeconds());
            if (durationSeconds != null && durationSeconds > 0) {
                history.setProgressPercent((int) ((double) history.getPositionSeconds() / durationSeconds * 100));
            }
            history.setCompleted(history.getProgressPercent() >= 90);
            history.setWatchCount(history.getWatchCount() + 1);
            history.setLastWatchedAt(Instant.now());
        } else {
            history = new WatchHistory();
            history.setUserId(userId);
            history.setVideoId(videoId);
            history.setPositionSeconds(positionSeconds != null ? positionSeconds : 0);
            history.setDurationSeconds(durationSeconds);
            if (durationSeconds != null && durationSeconds > 0) {
                history.setProgressPercent((int) ((double) history.getPositionSeconds() / durationSeconds * 100));
            }
            history.setCompleted(history.getProgressPercent() >= 90);
        }

        watchHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public PageResponse<WatchHistoryResponse> getLikedVideos(int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        List<Like> likes = likeRepository.findByUserId(userId);
        int from = Math.min(page * size, likes.size());
        int to = Math.min(from + size, likes.size());
        List<Like> slice = likes.subList(from, to);
        List<WatchHistoryResponse> items = slice.stream().map(like -> {
            Video video = videoRepository.findById(like.getVideoId()).orElse(null);
            return WatchHistoryResponse.builder()
                    .id(like.getId())
                    .videoId(like.getVideoId())
                    .videoTitle(video != null ? video.getTitle() : null)
                    .videoThumbnailUrl(video != null ? video.getThumbnailUrl() : null)
                    .channelName(null)
                    .channelSlug(null)
                    .positionSeconds(0)
                    .durationSeconds(video != null && video.getDurationSeconds() != null ? video.getDurationSeconds() : 0)
                    .progressPercent(0)
                    .completed(false)
                    .lastWatchedAt(like.getCreatedAt())
                    .watchCount(0)
                    .build();
        }).toList();
        return PageResponse.of(items, page, size, likes.size());
    }

    @Transactional(readOnly = true)
    public PageResponse<WatchHistoryResponse> getWatchHistory(int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchHistory> history = watchHistoryRepository.findByUserIdOrderByLastWatchedAtDesc(userId, pageable);

        // Would need to join with video info for title/thumbnail
        // For now, return basic info
        return PageResponse.of(
                history.getContent().stream()
                        .map(this::mapToWatchHistoryResponse)
                        .toList(),
                page, size, history.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public List<WatchHistoryResponse> getContinueWatching() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        List<WatchHistory> history = watchHistoryRepository.findByUserIdAndProgressPercentGreaterThan(userId, 10);
        return history.stream()
                .filter(h -> !h.isCompleted() || h.getWatchCount() > 1)
                .limit(20)
                .map(this::mapToWatchHistoryResponse)
                .toList();
    }

    private void publishVideoViewedEvent(UUID videoId, UUID userId, Integer position, Integer duration) {
        VideoViewedEvent event = VideoViewedEvent.builder()
                .videoId(videoId)
                .channelId(videoRepository.findById(videoId).map(Video::getChannelId).orElse(null))
                .userId(userId)
                .positionSeconds(position)
                .durationSeconds(duration)
                .timestamp(Instant.now())
                .build();

        EventEnvelope<VideoViewedEvent> envelope = EventEnvelope.create(
                "video.viewed", "v1", event, "engagement-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_VIEWED, videoId.toString(), envelope);
    }

    private void publishVideoLikedEvent(UUID videoId, UUID userId, boolean liked) {
        VideoLikedEvent event = VideoLikedEvent.builder()
                .videoId(videoId)
                .channelId(videoRepository.findById(videoId).map(Video::getChannelId).orElse(null))
                .userId(userId)
                .liked(liked)
                .timestamp(Instant.now())
                .build();

        EventEnvelope<VideoLikedEvent> envelope = EventEnvelope.create(
                "video.liked", "v1", event, "engagement-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_LIKED, videoId.toString(), envelope);
    }

    private void publishCommentCreatedEvent(Comment comment) {
        CommentCreatedEvent event = CommentCreatedEvent.builder()
                .commentId(comment.getId())
                .videoId(comment.getVideoId())
                .channelId(videoRepository.findById(comment.getVideoId()).map(Video::getChannelId).orElse(null))
                .userId(comment.getUserId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .timestamp(Instant.now())
                .build();

        EventEnvelope<CommentCreatedEvent> envelope = EventEnvelope.create(
                "comment.created", "v1", event, "engagement-service"
        );

        kafkaTemplate.send(KafkaTopicNames.COMMENT_CREATED, comment.getVideoId().toString(), envelope);
    }

    private CommentResponse mapToCommentResponse(Comment comment, UUID currentUserId) {
        return CommentResponse.builder()
                .id(comment.getId())
                .videoId(comment.getVideoId())
                .channelId(videoRepository.findById(comment.getVideoId()).map(Video::getChannelId).orElse(null))
                .userId(comment.getUserId())
                .username(null) // Would need user service call
                .userAvatarUrl(null)
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .hidden(comment.isHidden())
                .likeCount(comment.getLikeCount())
                .replyCount(comment.getReplyCount())
                .pinned(comment.isPinned())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(List.of())
                .isLiked(false)
                .isOwn(comment.getUserId().equals(currentUserId))
                .build();
    }

    private CommentResponse mapToCommentResponseWithReplies(Comment comment, UUID currentUserId) {
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
        return mapToCommentResponse(comment, currentUserId).toBuilder()
                .replies(replies.stream()
                        .map(r -> mapToCommentResponse(r, currentUserId))
                        .toList())
                .build();
    }

    private WatchHistoryResponse mapToWatchHistoryResponse(WatchHistory history) {
        return WatchHistoryResponse.builder()
                .id(history.getId())
                .videoId(history.getVideoId())
                .videoTitle(null) // Would need video service call
                .videoThumbnailUrl(null)
                .channelName(null)
                .channelSlug(null)
                .positionSeconds(history.getPositionSeconds())
                .durationSeconds(history.getDurationSeconds() != null ? history.getDurationSeconds() : 0)
                .progressPercent(history.getProgressPercent())
                .completed(history.isCompleted())
                .lastWatchedAt(history.getLastWatchedAt())
                .watchCount(history.getWatchCount())
                .build();
    }
}
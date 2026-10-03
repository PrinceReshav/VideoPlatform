package com.platform.subscription.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ConflictException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import com.platform.subscription.dto.FeedItemResponse;
import com.platform.subscription.dto.SubscribeRequest;
import com.platform.subscription.dto.SubscriptionResponse;
import com.platform.subscription.entity.Subscription;
import com.platform.subscription.event.SubscriptionCreatedEvent;
import com.platform.subscription.event.SubscriptionDeletedEvent;
import com.platform.subscription.repository.SubscriptionRepository;
import com.platform.user.entity.Channel;
import com.platform.user.repository.ChannelRepository;
import com.platform.user.repository.CreatorProfileRepository;
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
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ChannelRepository channelRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final VideoRepository videoRepository;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional
    public SubscriptionResponse subscribe(SubscribeRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        UUID channelId = request.getChannelId();

        if (userId.equals(getChannelOwner(channelId))) {
            throw new BusinessException(ApiErrorCodes.CANNOT_SUBSCRIBE_SELF,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot subscribe to your own channel");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CHANNEL_NOT_FOUND, "Channel not found"));

        if (subscriptionRepository.existsByUserIdAndChannelId(userId, channelId)) {
            throw new ConflictException(ApiErrorCodes.ALREADY_SUBSCRIBED, "Already subscribed to this channel");
        }

        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setChannelId(channelId);
        subscription.setNotificationsEnabled(request.isNotificationsEnabled());
        subscription = subscriptionRepository.save(subscription);

        // Increment subscriber count on channel
        channel.setSubscriberCount(channel.getSubscriberCount() + 1);
        channelRepository.save(channel);

        // Publish event
        publishSubscriptionCreatedEvent(subscription, channel);

        log.info("User {} subscribed to channel {}", userId, channelId);
        return mapToSubscriptionResponse(subscription, channel);
    }

    @Transactional
    public void unsubscribe(UUID channelId) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        Subscription subscription = subscriptionRepository.findByUserIdAndChannelId(userId, channelId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_SUBSCRIBED, "Not subscribed to this channel"));

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CHANNEL_NOT_FOUND, "Channel not found"));

        subscriptionRepository.delete(subscription);

        // Decrement subscriber count
        channel.setSubscriberCount(Math.max(0, channel.getSubscriberCount() - 1));
        channelRepository.save(channel);

        // Publish event
        publishSubscriptionDeletedEvent(subscription.getId(), userId, channelId);

        log.info("User {} unsubscribed from channel {}", userId, channelId);
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(UUID channelId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        return subscriptionRepository.existsByUserIdAndChannelId(userId, channelId);
    }

    @Transactional(readOnly = true)
    public long getSubscriberCount(UUID channelId) {
        return subscriptionRepository.countByChannelId(channelId);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> getMySubscriptions(int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Subscription> subscriptions = subscriptionRepository.findByUserId(userId, pageable);

        return PageResponse.of(
                subscriptions.getContent().stream()
                        .map(s -> mapToSubscriptionResponse(s, channelRepository.findById(s.getChannelId()).orElse(null)))
                        .toList(),
                page, size, subscriptions.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> getChannelSubscribers(UUID channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Subscription> subscriptions = subscriptionRepository.findByChannelId(channelId, pageable);

        return PageResponse.of(
                subscriptions.getContent().stream()
                        .map(s -> mapToSubscriptionResponse(s, channelRepository.findById(s.getChannelId()).orElse(null)))
                        .toList(),
                page, size, subscriptions.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public List<UUID> getSubscribedChannelIds() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        return subscriptionRepository.findSubscribedChannelIds(userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedItemResponse> getSubscriptionFeed(int page, int size) {
        List<UUID> channelIds = getSubscribedChannelIds();
        if (channelIds.isEmpty()) return PageResponse.of(List.of(), page, size, 0);
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videos = videoRepository.findByChannelIdInAndPublicationStateAndProcessingStateAndBlockedFalseAndDeletedFalse(
                channelIds, Video.PublicationState.PUBLISHED, Video.ProcessingState.READY, pageable);
        List<FeedItemResponse> items = videos.getContent().stream().map(video -> {
            var channel = channelRepository.findById(video.getChannelId()).orElse(null);
            return FeedItemResponse.builder()
                    .videoId(video.getId()).title(video.getTitle()).thumbnailUrl(video.getThumbnailUrl())
                    .durationSeconds(video.getDurationSeconds())
                    .channelName(channel != null ? channel.getName() : null)
                    .channelSlug(channel != null ? channel.getSlug() : null)
                    .channelAvatarUrl(channel != null ? channel.getAvatarUrl() : null)
                    .viewCount(video.getViewCount()).publishedAt(video.getPublishedAt()).build();
        }).toList();
        return PageResponse.of(items, page, size, videos.getTotalElements());
    }

    private UUID getChannelOwner(UUID channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CHANNEL_NOT_FOUND, "Channel not found"));
        return creatorProfileRepository.findById(channel.getCreatorProfileId())
                .map(com.platform.user.entity.CreatorProfile::getUserId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "Creator profile not found"));
    }

    private void publishSubscriptionCreatedEvent(Subscription subscription, Channel channel) {
        SubscriptionCreatedEvent event = SubscriptionCreatedEvent.builder()
                .subscriptionId(subscription.getId())
                .userId(subscription.getUserId())
                .channelId(subscription.getChannelId())
                .timestamp(Instant.now())
                .build();

        EventEnvelope<SubscriptionCreatedEvent> envelope = EventEnvelope.create(
                "subscription.created", "v1", event, "subscription-service"
        );

        kafkaTemplate.send(KafkaTopicNames.SUBSCRIPTION_CREATED, subscription.getChannelId().toString(), envelope);
    }

    private void publishSubscriptionDeletedEvent(UUID subscriptionId, UUID userId, UUID channelId) {
        SubscriptionDeletedEvent event = SubscriptionDeletedEvent.builder()
                .subscriptionId(subscriptionId)
                .userId(userId)
                .channelId(channelId)
                .timestamp(Instant.now())
                .build();

        EventEnvelope<SubscriptionDeletedEvent> envelope = EventEnvelope.create(
                "subscription.deleted", "v1", event, "subscription-service"
        );

        kafkaTemplate.send(KafkaTopicNames.SUBSCRIPTION_DELETED, channelId.toString(), envelope);
    }

    private SubscriptionResponse mapToSubscriptionResponse(Subscription subscription, Channel channel) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .channelId(subscription.getChannelId())
                .channelName(channel != null ? channel.getName() : null)
                .channelSlug(channel != null ? channel.getSlug() : null)
                .channelAvatarUrl(channel != null ? channel.getAvatarUrl() : null)
                .notificationsEnabled(subscription.isNotificationsEnabled())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
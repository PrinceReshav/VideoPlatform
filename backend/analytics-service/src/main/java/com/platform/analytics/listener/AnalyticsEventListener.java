package com.platform.analytics.listener;

import com.platform.analytics.service.AnalyticsService;
import com.platform.common.event.CommentCreatedEvent;
import com.platform.common.event.VideoLikedEvent;
import com.platform.common.event.VideoViewedEvent;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {
    private final AnalyticsService analyticsService;

    @KafkaListener(topics = KafkaTopicNames.VIDEO_VIEWED, groupId = "analytics-service")
    public void onVideoViewed(@Payload EventEnvelope<VideoViewedEvent> envelope) {
        VideoViewedEvent event = envelope.getPayload();
        if (event == null) return;
        analyticsService.recordVideoView(event.getVideoId(), event.getChannelId(), toDate(event.getTimestamp(), envelope.getTimestamp()), event.getPositionSeconds());
    }

    @KafkaListener(topics = KafkaTopicNames.VIDEO_LIKED, groupId = "analytics-service")
    public void onVideoLiked(@Payload EventEnvelope<VideoLikedEvent> envelope) {
        VideoLikedEvent event = envelope.getPayload();
        if (event == null) return;
        analyticsService.recordVideoLike(event.getVideoId(), event.getChannelId(), toDate(event.getTimestamp(), envelope.getTimestamp()), event.isLiked());
    }

    @KafkaListener(topics = KafkaTopicNames.COMMENT_CREATED, groupId = "analytics-service")
    public void onCommentCreated(@Payload EventEnvelope<CommentCreatedEvent> envelope) {
        CommentCreatedEvent event = envelope.getPayload();
        if (event == null) return;
        analyticsService.recordComment(event.getVideoId(), event.getChannelId(), toDate(event.getTimestamp(), envelope.getTimestamp()));
    }

    private LocalDate toDate(Instant eventTimestamp, Instant envelopeTimestamp) {
        Instant timestamp = eventTimestamp != null ? eventTimestamp : (envelopeTimestamp != null ? envelopeTimestamp : Instant.now());
        return timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

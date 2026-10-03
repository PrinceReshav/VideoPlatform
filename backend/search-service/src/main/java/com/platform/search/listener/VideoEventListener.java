package com.platform.search.service;

import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.event.VideoDeletedEvent;
import com.platform.common.event.VideoPublishedEvent;
import com.platform.common.event.VideoUnpublishedEvent;
import com.platform.search.service.VideoSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEventListener {

    private final VideoSearchService videoSearchService;

    @KafkaListener(topics = KafkaTopicNames.VIDEO_PUBLISHED, groupId = "search-service")
    public void handleVideoPublished(@Payload EventEnvelope<VideoPublishedEvent> envelope) {
        VideoPublishedEvent event = envelope.getPayload();
        log.info("Received video published event for indexing: {}", event.getVideoId());
        videoSearchService.indexVideo(event);
    }

    @KafkaListener(topics = KafkaTopicNames.VIDEO_UNPUBLISHED, groupId = "search-service")
    public void handleVideoUnpublished(@Payload EventEnvelope<VideoUnpublishedEvent> envelope) {
        VideoUnpublishedEvent event = envelope.getPayload();
        log.info("Received video unpublished event for unindexing: {}", event.getVideoId());
        videoSearchService.unindexVideo(event.getVideoId().toString());
    }

    @KafkaListener(topics = KafkaTopicNames.VIDEO_DELETED, groupId = "search-service")
    public void handleVideoDeleted(@Payload EventEnvelope<VideoDeletedEvent> envelope) {
        VideoDeletedEvent event = envelope.getPayload();
        log.info("Received video deleted event for index deletion: {}", event.getVideoId());
        videoSearchService.deleteVideoIndex(event.getVideoId().toString());
    }
}
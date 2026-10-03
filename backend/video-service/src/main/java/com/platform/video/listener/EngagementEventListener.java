package com.platform.video.listener;

import com.platform.common.event.VideoViewedEvent;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngagementEventListener {
    private final VideoRepository videoRepository;

    @Transactional
    @KafkaListener(topics = KafkaTopicNames.VIDEO_VIEWED, groupId = "video-service-views")
    public void handleVideoViewed(@Payload EventEnvelope<VideoViewedEvent> envelope) {
        VideoViewedEvent event = envelope.getPayload();
        if (event == null || event.getVideoId() == null) return;
        videoRepository.incrementViewCount(event.getVideoId());
    }
}

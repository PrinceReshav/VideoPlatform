package com.platform.search.listener;

import com.platform.common.event.CommentCreatedEvent;
import com.platform.common.event.VideoLikedEvent;
import com.platform.common.event.VideoViewedEvent;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.search.document.VideoSearchDocument;
import com.platform.search.repository.VideoSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EngagementSearchEventListener {
    private final VideoSearchRepository repository;

    @KafkaListener(topics = KafkaTopicNames.VIDEO_VIEWED, groupId = "search-service-engagement")
    public void handleView(@Payload EventEnvelope<VideoViewedEvent> envelope) {
        if (envelope.getPayload() == null) return;
        repository.findByVideoId(envelope.getPayload().getVideoId().toString()).ifPresent(doc -> {
            doc.setViewCount((doc.getViewCount() == null ? 0 : doc.getViewCount()) + 1);
            repository.save(doc);
        });
    }

    @KafkaListener(topics = KafkaTopicNames.VIDEO_LIKED, groupId = "search-service-engagement")
    public void handleLike(@Payload EventEnvelope<VideoLikedEvent> envelope) {
        if (envelope.getPayload() == null) return;
        repository.findByVideoId(envelope.getPayload().getVideoId().toString()).ifPresent(doc -> {
            long current = doc.getLikeCount() == null ? 0 : doc.getLikeCount();
            doc.setLikeCount((int) Math.max(0, current + (envelope.getPayload().isLiked() ? 1 : -1)));
            repository.save(doc);
        });
    }

    @KafkaListener(topics = KafkaTopicNames.COMMENT_CREATED, groupId = "search-service-engagement")
    public void handleComment(@Payload EventEnvelope<CommentCreatedEvent> envelope) {
        if (envelope.getPayload() == null) return;
        repository.findByVideoId(envelope.getPayload().getVideoId().toString()).ifPresent(doc -> {
            doc.setCommentCount((doc.getCommentCount() == null ? 0 : doc.getCommentCount()) + 1);
            repository.save(doc);
        });
    }
}

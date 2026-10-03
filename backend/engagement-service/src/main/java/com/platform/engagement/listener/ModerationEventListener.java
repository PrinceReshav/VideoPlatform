package com.platform.engagement.listener;

import com.platform.common.event.ModerationActionAppliedEvent;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.engagement.entity.Comment;
import com.platform.engagement.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ModerationEventListener {
    private final CommentRepository commentRepository;

    @KafkaListener(topics = KafkaTopicNames.MODERATION_ACTION_APPLIED, groupId = "engagement-service-moderation")
    public void handle(@Payload EventEnvelope<ModerationActionAppliedEvent> envelope) {
        ModerationActionAppliedEvent event = envelope.getPayload();
        if (event == null || !"COMMENT".equals(event.getTargetType())) return;
        commentRepository.findById(event.getTargetId()).ifPresent(comment -> {
            switch (event.getActionType()) {
                case "HIDE_CONTENT", "DELETE_CONTENT" -> {
                    comment.setHidden(true);
                    comment.setHiddenBy(event.getModeratorId());
                    comment.setHiddenAt(Instant.now());
                    comment.setHiddenReason(event.getReason());
                }
                case "RESTORE_CONTENT" -> {
                    comment.setHidden(false);
                    comment.setHiddenBy(null);
                    comment.setHiddenAt(null);
                    comment.setHiddenReason(null);
                }
                default -> { return; }
            }
            commentRepository.save(comment);
        });
    }
}

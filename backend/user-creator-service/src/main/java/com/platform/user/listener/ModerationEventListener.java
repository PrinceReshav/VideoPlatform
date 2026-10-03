package com.platform.user.listener;

import com.platform.common.event.ModerationActionAppliedEvent;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.user.repository.ChannelRepository;
import com.platform.user.repository.CreatorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ModerationEventListener {
    private final ChannelRepository channelRepository;
    private final CreatorProfileRepository creatorProfileRepository;

    @KafkaListener(topics = KafkaTopicNames.MODERATION_ACTION_APPLIED, groupId = "user-creator-service-moderation")
    public void handle(@Payload EventEnvelope<ModerationActionAppliedEvent> envelope) {
        ModerationActionAppliedEvent event = envelope.getPayload();
        if (event == null) return;
        if ("CHANNEL".equals(event.getTargetType())) {
            channelRepository.findById(event.getTargetId()).ifPresent(channel -> {
                if ("RESTORE_CONTENT".equals(event.getActionType())) {
                    channel.setSuspended(false);
                    channel.setSuspendedAt(null);
                    channel.setSuspendedBy(null);
                } else if ("TEMPORARY_SUSPENSION".equals(event.getActionType()) || "PERMANENT_SUSPENSION".equals(event.getActionType()) || "CHANNEL_TERMINATION".equals(event.getActionType())) {
                    channel.setSuspended(true);
                    channel.setSuspendedAt(Instant.now());
                    channel.setSuspendedBy(event.getModeratorId());
                }
                channelRepository.save(channel);
            });
        } else if ("USER".equals(event.getTargetType())) {
            creatorProfileRepository.findByUserId(event.getTargetId()).ifPresent(profile -> {
                if ("RESTORE_CONTENT".equals(event.getActionType())) {
                    profile.setSuspended(false);
                    profile.setSuspendedAt(null);
                    profile.setSuspendedBy(null);
                } else if (event.getActionType().contains("SUSPENSION") || event.getActionType().contains("TERMINATION")) {
                    profile.setSuspended(true);
                    profile.setSuspendedAt(Instant.now());
                    profile.setSuspendedBy(event.getModeratorId());
                }
                creatorProfileRepository.save(profile);
            });
        }
    }
}

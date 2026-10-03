package com.platform.video.listener;

import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.event.ProcessingCompletedEvent;
import com.platform.common.event.ProcessingFailedEvent;
import com.platform.common.event.ModerationActionAppliedEvent;
import com.platform.common.event.NotificationRequestedEvent;
import com.platform.common.notification.NotificationType;
import com.platform.common.entity.VideoFileInfo;
import com.platform.common.entity.Video;
import com.platform.video.repository.VideoRepository;
import com.platform.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodingEventListener {

    private final VideoRepository videoRepository;
    private final VideoService videoService;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    @KafkaListener(topics = KafkaTopicNames.VIDEO_PROCESSING_COMPLETED, groupId = "video-service")
    public void handleProcessingCompleted(@Payload EventEnvelope<ProcessingCompletedEvent> envelope) {
        ProcessingCompletedEvent event = envelope.getPayload();
        log.info("Received processing completed event for video: {}", event.getVideoId());

        Video video = videoRepository.findById(event.getVideoId())
                .orElse(null);

        if (video == null) {
            log.warn("Video not found for processing completed event: {}", event.getVideoId());
            return;
        }

        video.markProcessingCompleted(
                event.getHlsManifestKey(),
                event.getResolutions(),
                event.getDurationSeconds(),
                event.getThumbnailUrl()
        );

        // Save video files
        if (event.getVideoFiles() != null) {
            for (VideoFileInfo fileInfo : event.getVideoFiles()) {
                videoService.saveVideoFile(video.getId(), fileInfo);
            }
        }

        videoRepository.save(video);
        publishProcessingNotification(video, NotificationType.PROCESSING_COMPLETE, "Video processing complete", "Your video \"" + video.getTitle() + "\" is ready.");
        log.info("Video {} marked as ready", video.getId());
    }

    @KafkaListener(topics = KafkaTopicNames.VIDEO_PROCESSING_FAILED, groupId = "video-service")
    public void handleProcessingFailed(@Payload EventEnvelope<ProcessingFailedEvent> envelope) {
        ProcessingFailedEvent event = envelope.getPayload();
        log.error("Received processing failed event for video: {} - {}", event.getVideoId(), event.getErrorMessage());

        Video video = videoRepository.findById(event.getVideoId())
                .orElse(null);

        if (video == null) {
            log.warn("Video not found for processing failed event: {}", event.getVideoId());
            return;
        }

        video.markProcessingFailed(event.getErrorMessage());
        videoRepository.save(video);
        publishProcessingNotification(video, NotificationType.PROCESSING_FAILED, "Video processing failed", "Processing failed for your video \"" + video.getTitle() + "\".");
    }
    @KafkaListener(topics = KafkaTopicNames.MODERATION_ACTION_APPLIED, groupId = "video-service-moderation")
    public void handleModerationAction(@Payload EventEnvelope<ModerationActionAppliedEvent> envelope) {
        ModerationActionAppliedEvent event = envelope.getPayload();
        if (event == null || !"VIDEO".equals(event.getTargetType())) return;
        videoRepository.findById(event.getTargetId()).ifPresent(video -> {
            switch (event.getActionType()) {
                case "HIDE_CONTENT", "DELETE_CONTENT", "CHANNEL_TERMINATION" -> video.block(event.getModeratorId(), event.getReason());
                case "RESTORE_CONTENT" -> video.unblock();
                default -> { return; }
            }
            videoRepository.save(video);
        });
    }

    private void publishProcessingNotification(Video video, NotificationType type, String title, String message) {
        NotificationRequestedEvent notification = NotificationRequestedEvent.builder()
                .notificationId(java.util.UUID.randomUUID())
                .userId(video.getCreatorId())
                .type(type)
                .title(title)
                .message(message)
                .referenceId(video.getId())
                .referenceType("VIDEO")
                .priority(1)
                .timestamp(java.time.Instant.now())
                .build();
        kafkaTemplate.send(KafkaTopicNames.NOTIFICATION_REQUESTED, video.getCreatorId().toString(),
                EventEnvelope.create("notification.requested", "v1", notification, "video-service"));
    }

}
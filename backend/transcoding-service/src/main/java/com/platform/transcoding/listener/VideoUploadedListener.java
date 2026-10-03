package com.platform.transcoding.listener;

import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.event.UploadCompletedEvent;
import com.platform.transcoding.service.TranscodingOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadedListener {

    private final TranscodingOrchestrator transcodingOrchestrator;

    @KafkaListener(topics = KafkaTopicNames.VIDEO_UPLOADED, groupId = "transcoding-service")
    public void handleVideoUploaded(@Payload EventEnvelope<UploadCompletedEvent> envelope) {
        UploadCompletedEvent event = envelope.getPayload();
        log.info("Received video uploaded event for video: {}", event.getVideoId());

        // Video-service creates the processing job in response to the same upload event.
        // Resolve the job through the event's video ID and start it asynchronously.
        // The orchestrator owns retry/failure handling.
        transcodingOrchestrator.startLatestPendingJob(event.getVideoId());
    }
}
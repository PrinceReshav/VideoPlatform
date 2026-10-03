package com.platform.video.listener;

import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.event.UploadCompletedEvent;
import com.platform.common.entity.Video;
import com.platform.common.entity.ProcessingJob;
import com.platform.video.repository.VideoRepository;
import com.platform.video.repository.ProcessingJobRepository;
import com.platform.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadCompletedListener {

    private final VideoRepository videoRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final VideoService videoService;

    @KafkaListener(topics = KafkaTopicNames.VIDEO_UPLOADED, groupId = "video-service")
    public void handleUploadCompleted(@Payload EventEnvelope<UploadCompletedEvent> envelope) {
        UploadCompletedEvent event = envelope.getPayload();
        log.info("Received upload completed event for video: {}", event.getVideoId());

        Video video = videoRepository.findById(event.getVideoId())
                .orElse(null);

        if (video == null) {
            log.warn("Video not found for upload completed event: {}", event.getVideoId());
            return;
        }

        // Update video with upload info
        video.setSourceObjectKey(event.getObjectKey());
        video.setSourceFileSize(event.getFileSize());
        video.setSourceChecksum(event.getChecksum());
        video.setProcessingState(Video.ProcessingState.UPLOADED);
        videoRepository.save(video);

        // Create processing job
        ProcessingJob job = new ProcessingJob();
        job.setVideoId(video.getId());
        job.setSourceObjectKey(event.getObjectKey());
        job.setSourceBucket("videos");
        job.setOutputBucket("hls");
        job.setOutputPrefix("hls/" + video.getId());
        job.setStatus(ProcessingJob.Status.PENDING);
        job.setPriority(0);
        job = processingJobRepository.save(job);

        video.setTranscodingJobId(job.getId());
        videoRepository.save(video);

        log.info("Created processing job {} for video {}", job.getId(), video.getId());
    }
}
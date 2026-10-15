package com.platform.transcoding.service;

import com.platform.common.entity.ProcessingJob;
import com.platform.common.entity.VideoFileInfo;
import com.platform.common.entity.FileType;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.repository.ProcessingJobRepository;
import com.platform.common.event.ProcessingCompletedEvent;
import com.platform.common.event.ProcessingFailedEvent;
import com.platform.common.event.ProcessingStartedEvent;
import com.platform.transcoding.model.ResolutionLadder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodingOrchestrator {

    private final ProcessingJobRepository processingJobRepository;
    private final FfmpegService ffmpegService;
    private final com.platform.common.storage.StorageProvider storageProvider;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Transactional
    public void startLatestPendingJob(UUID videoId) {
        startLatestPendingJob(videoId, 0);
    }

    private void startLatestPendingJob(UUID videoId, int attempt) {
        processingJobRepository.findByVideoIdAndStatusOrderByCreatedAtDesc(videoId, ProcessingJob.Status.PENDING)
                .stream().findFirst().ifPresentOrElse(
                        job -> processJob(job.getId()),
                        () -> {
                            if (attempt < 5) {
                                CompletableFuture.delayedExecutor(2, java.util.concurrent.TimeUnit.SECONDS)
                                        .execute(() -> startLatestPendingJob(videoId, attempt + 1));
                            } else {
                                log.error("No pending processing job found for uploaded video {} after retries", videoId);
                            }
                        });
    }

    @Transactional
    public void processJob(UUID jobId) {
        CompletableFuture.runAsync(() -> {
            try {
                doProcessJob(jobId);
            } catch (Exception e) {
                log.error("Transcoding job {} failed with exception", jobId, e);
                handleJobFailure(jobId, e.getMessage());
            }
        }, executor);
    }

    private void doProcessJob(UUID jobId) throws IOException, InterruptedException {
        ProcessingJob job = processingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        log.info("Starting transcoding job: {}", jobId);

        job.setStatus(ProcessingJob.Status.PROCESSING);
        job.setWorkerId(getWorkerId());
        job.setStartedAt(Instant.now());
        processingJobRepository.save(job);

        // Publish started event
        publishStartedEvent(job);

        // Download source video from MinIO
        Path tempDir = Paths.get("/tmp/transcoding", jobId.toString());
        Files.createDirectories(tempDir);
        Path inputPath = tempDir.resolve("source.mp4");

        try {
            log.info("Downloading source video for job {}", jobId);
            var sourceStream = storageProvider.getObjectStream(job.getSourceBucket(), job.getSourceObjectKey());
            if (sourceStream.isEmpty()) {
                throw new IOException("Source video not found in storage");
            }
            try (var stream = sourceStream.get()) {
                Files.copy(stream, inputPath);
            }
        } catch (IOException e) {
            throw new IOException("Failed to download source video: " + e.getMessage(), e);
        }

        // Probe video to get resolution
        FfmpegService.VideoInfo videoInfo = ffmpegService.probeVideo(inputPath);
        log.info("Video info: {}x{}, duration: {}s, codec: {}", videoInfo.width(), videoInfo.height(), videoInfo.durationSeconds(), videoInfo.videoCodec());

        // Determine resolution ladder
        ResolutionLadder ladder = ResolutionLadder.defaultLadder(videoInfo.width(), videoInfo.height());

        // Update job with resolutions
        job.setResolutions(ladder.getResolutions().stream()
                .map(r -> r.getName())
                .collect(java.util.stream.Collectors.joining(",")));
        processingJobRepository.save(job);

        // Transcode
        List<VideoFileInfo> resultFiles = ffmpegService.transcodeToHls(
                job.getVideoId(),
                inputPath,
                job.getOutputBucket(),
                job.getOutputPrefix(),
                ladder,
                storageProvider
        );

        // Extract or generate captions
        List<VideoFileInfo> captionFiles = new ArrayList<>();
        try {
            // First, try to extract existing captions from the source video
            List<FfmpegService.CaptionInfo> captionInfos = ffmpegService.probeCaptions(inputPath);
            if (!captionInfos.isEmpty()) {
                log.info("Found {} embedded caption streams in video {}", captionInfos.size(), job.getVideoId());
                captionFiles = ffmpegService.extractCaptions(
                        job.getVideoId(),
                        inputPath,
                        job.getOutputBucket(),
                        job.getOutputPrefix(),
                        captionInfos,
                        storageProvider
                );
            } else {
                // If no embedded captions found, generate captions using speech-to-text
                // For now, we'll generate English captions as a placeholder
                log.info("No embedded captions found in video {}, generating captions", job.getVideoId());
                List<String> languages = List.of("en"); // Generate English captions
                captionFiles = ffmpegService.generateCaptions(
                        job.getVideoId(),
                        inputPath,
                        job.getOutputBucket(),
                        job.getOutputPrefix(),
                        languages,
                        storageProvider
                );
            }
        } catch (Exception e) {
            log.warn("Failed to process captions for video {}: {}", job.getVideoId(), e.getMessage());
            // Continue without captions - this is not a critical failure
        }

        // Add caption files to result files
        resultFiles.addAll(captionFiles);

        // Extract or generate dubbed audio
        List<VideoFileInfo> dubbedAudioFiles = new ArrayList<>();
        try {
            // First, try to extract existing dubbed audio from the source video
            List<FfmpegService.DubbedAudioInfo> dubbedAudioInfos = ffmpegService.probeDubbedAudio(inputPath);
            if (!dubbedAudioInfos.isEmpty()) {
                log.info("Found {} dubbed audio streams in video {}", dubbedAudioInfos.size(), job.getVideoId());
                dubbedAudioFiles = ffmpegService.extractDubbedAudio(
                        job.getVideoId(),
                        inputPath,
                        job.getOutputBucket(),
                        job.getOutputPrefix(),
                        dubbedAudioInfos,
                        storageProvider
                );
            } else {
                // If no embedded dubbed audio found, generate dubbed audio using text-to-speech
                // For now, we'll generate some common languages as a placeholder
                log.info("No embedded dubbed audio found in video {}, generating dubbed audio", job.getVideoId());
                List<String> languages = List.of("es", "fr"); // Generate Spanish and French dubbed audio
                dubbedAudioFiles = ffmpegService.generateDubbedAudio(
                        job.getVideoId(),
                        inputPath,
                        job.getOutputBucket(),
                        job.getOutputPrefix(),
                        languages,
                        storageProvider
                );
            }
        } catch (Exception e) {
            log.warn("Failed to process dubbed audio for video {}: {}", job.getVideoId(), e.getMessage());
            // Continue without dubbed audio - this is not a critical failure
        }

        // Add dubbed audio files to result files
        resultFiles.addAll(dubbedAudioFiles);

        // Update job as completed
        job.setStatus(ProcessingJob.Status.COMPLETED);
        job.setCompletedAt(Instant.now());
        job.setProgress(100);
        processingJobRepository.save(job);

        // Publish completed event
        publishCompletedEvent(job, resultFiles, videoInfo.durationSeconds());

        log.info("Transcoding job {} completed successfully with {} output files", jobId, resultFiles.size());
    }

    private void handleJobFailure(UUID jobId, String errorMessage) {
        try {
            ProcessingJob job = processingJobRepository.findById(jobId).orElse(null);
            if (job != null) {
                if (job.getRetryCount() < job.getMaxRetries()) {
                    job.setRetryCount(job.getRetryCount() + 1);
                    job.setStatus(ProcessingJob.Status.PENDING);
                    job.setErrorMessage(errorMessage);
                    processingJobRepository.save(job);
                    log.warn("Job {} failed, scheduling retry {}/{}", jobId, job.getRetryCount(), job.getMaxRetries());
                } else {
                    job.setStatus(ProcessingJob.Status.FAILED);
                    job.setCompletedAt(Instant.now());
                    job.setErrorMessage(errorMessage);
                    processingJobRepository.save(job);
                    publishFailedEvent(job, errorMessage);
                    log.error("Job {} failed permanently after {} retries", jobId, job.getMaxRetries());
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle job failure for {}", jobId, e);
        }
    }

    private void publishStartedEvent(ProcessingJob job) {
        ProcessingStartedEvent event = ProcessingStartedEvent.builder()
                .videoId(job.getVideoId())
                .jobId(job.getId())
                .sourceKey(job.getSourceObjectKey())
                .resolutions(java.util.Arrays.asList(job.getResolutions() != null ? job.getResolutions().split(",") : new String[0]))
                .startedAt(Instant.now())
                .build();

        EventEnvelope<ProcessingStartedEvent> envelope = EventEnvelope.create(
                "video.processing.started", "v1", event, "transcoding-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_PROCESSING_STARTED, job.getVideoId().toString(), envelope);
    }

    private void publishCompletedEvent(ProcessingJob job, List<VideoFileInfo> files, int durationSeconds) {
        // Find master manifest
        String hlsManifestKey = files.stream()
                .filter(f -> f.getFileType() == FileType.HLS_MANIFEST && f.isMaster())
                .findFirst()
                .map(VideoFileInfo::getObjectKey)
                .orElse(null);

        // Find thumbnail
        String thumbnailUrl = files.stream()
                .filter(f -> f.getFileType() == FileType.THUMBNAIL)
                .findFirst()
                .map(f -> storageProvider.generatePresignedGetUrl(f.getBucket(), f.getObjectKey(), 4 * 60 * 60))
                .orElse(null);

        ProcessingCompletedEvent event = ProcessingCompletedEvent.builder()
                .videoId(job.getVideoId())
                .jobId(job.getId())
                .hlsManifestKey(hlsManifestKey)
                .resolutions(files.stream()
                        .filter(f -> f.getFileType() == FileType.HLS_MANIFEST && !f.isMaster())
                        .map(VideoFileInfo::getResolution)
                        .collect(java.util.stream.Collectors.toSet()))
                .durationSeconds(durationSeconds)
                .thumbnailUrl(thumbnailUrl)
                .videoFiles(files)
                .completedAt(Instant.now())
                .build();

        EventEnvelope<ProcessingCompletedEvent> envelope = EventEnvelope.create(
                "video.processing.completed", "v1", event, "transcoding-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_PROCESSING_COMPLETED, job.getVideoId().toString(), envelope);
    }

    private void publishFailedEvent(ProcessingJob job, String errorMessage) {
        ProcessingFailedEvent event = ProcessingFailedEvent.builder()
                .videoId(job.getVideoId())
                .jobId(job.getId())
                .errorCode("TRANSCODING_FAILED")
                .errorMessage(errorMessage)
                .retryable(job.getRetryCount() < job.getMaxRetries())
                .failedAt(Instant.now())
                .build();

        EventEnvelope<ProcessingFailedEvent> envelope = EventEnvelope.create(
                "video.processing.failed", "v1", event, "transcoding-service"
        );

        kafkaTemplate.send(KafkaTopicNames.VIDEO_PROCESSING_FAILED, job.getVideoId().toString(), envelope);
    }

    private String getWorkerId() {
        return "transcoder-" + java.net.InetAddress.getLoopbackAddress().getHostAddress() + "-" + ProcessHandle.current().pid();
    }
}
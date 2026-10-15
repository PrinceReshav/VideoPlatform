package com.platform.moderationvideo.service;

import com.platform.common.entity.Video;
import com.platform.common.event.UploadCompletedEvent;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.repository.VideoRepository;
import com.platform.common.storage.StorageProvider;
import com.platform.moderationvideo.dto.ModerationResult;
import com.platform.moderationvideo.dto.VideoModerationResult;
import com.platform.moderationvideo.util.ContentAnalyzer;
import com.platform.moderationvideo.util.FfmpegFrameExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoModerationService {

    private final VideoRepository videoRepository;
    private final StorageProvider storageProvider;
    private final FfmpegFrameExtractor frameExtractor;
    private final ContentAnalyzer contentAnalyzer;

    @Value("${storage.minio.bucket-name:videos}")
    private String defaultBucket;

    @Value("${moderation.frame-sampling-fps:2.0}")
    private double frameSamplingFps;

    @Value("${moderation.max-sampled-frames:600}")
    private int maxSampledFrames;

    @Value("${moderation.explicit-content-threshold:0.70}")
    private double explicitContentThreshold;

    @Value("${moderation.temp-dir:${java.io.tmpdir}/video-moderation}")
    private String tempDir;

    @Value("${moderation.block-on-error:true}")
    private boolean blockOnError;

    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @KafkaListener(
            topics = KafkaTopicNames.VIDEO_UPLOADED,
            groupId = "${spring.kafka.consumer.group-id:video-moderation-service}"
    )
    public void handleVideoUploaded(
            @Payload EventEnvelope<UploadCompletedEvent> envelope) {

        if (envelope == null || envelope.getPayload() == null) {
            log.warn("Ignoring empty upload event");
            return;
        }

        UploadCompletedEvent event = envelope.getPayload();
        UUID videoId = event.getVideoId();

        if (videoId == null
                || event.getObjectKey() == null
                || event.getObjectKey().isBlank()) {

            log.error(
                    "Ignoring malformed upload event: videoId={}, objectKey={}",
                    videoId,
                    event.getObjectKey()
            );
            return;
        }

        Video video = videoRepository.findById(videoId).orElse(null);

        if (video == null) {
            log.error(
                    "Video {} from upload event does not exist",
                    videoId
            );
            return;
        }

        if (video.isBlocked()) {
            log.info(
                    "Video {} is already blocked; moderation skipped",
                    videoId
            );
            return;
        }

        Path localFile = null;
        video.setModerationState(Video.ModerationState.PENDING);
        videoRepository.save(video);

        try {
            /*
             * Download the uploaded object locally and verify its
             * size/checksum before handing it to FFmpeg.
             */
            localFile = downloadAndVerify(event);

            /*
             * JavaCV uses the Bytedeco OpenCV Mat type:
             *
             * org.bytedeco.opencv.opencv_core.Mat
             *
             * Do not use org.opencv.core.Mat here.
             */
            List<org.bytedeco.opencv.opencv_core.Mat> frames =
                    frameExtractor.extractFrames(
                            localFile.toFile(),
                            frameSamplingFps,
                            maxSampledFrames
                    );

            try {
                VideoModerationResult analysis =
                        contentAnalyzer.analyzeFrames(
                                frames,
                                Files.size(localFile)
                        );

                ModerationResult result =
                        ModerationResult.builder()
                                .explicitContentDetected(
                                        analysis.isExplicitContentDetected()
                                )
                                .confidence(
                                        analysis.getConfidence()
                                )
                                .details(
                                        analysis.getDetails()
                                )
                                .build();

                if (result.isExplicitContentDetected()
                        && result.getConfidence()
                        >= explicitContentThreshold) {

                    blockVideo(
                            video,
                            result.getDetails(),
                            result.getConfidence()
                    );
                    video.setModerationState(Video.ModerationState.BLOCKED);
                    videoRepository.save(video);

                    log.warn(
                            "Video {} BLOCKED by moderation. confidence={}",
                            videoId,
                            result.getConfidence()
                    );

                } else {
                    video.setModerationState(Video.ModerationState.PASSED);
                    videoRepository.save(video);
                    log.info(
                            "Video {} passed moderation. confidence={}",
                            videoId,
                            result.getConfidence()
                    );
                }

            } finally {
                /*
                 * Bytedeco Mat owns native memory, so release every
                 * extracted frame after analysis.
                 */
                frameExtractor.release(frames);
            }

        } catch (Exception e) {

            log.error(
                    "Moderation failed for video {}",
                    videoId,
                    e
            );

            if (blockOnError) {
                blockVideoOnError(video, e);
                video.setModerationState(Video.ModerationState.ERROR);
                videoRepository.save(video);
            } else {
                video.setModerationState(Video.ModerationState.ERROR);
                videoRepository.save(video);
            }

        } finally {
            deleteQuietly(localFile);
        }
    }

    private Path downloadAndVerify(
            UploadCompletedEvent event) throws Exception {

        String bucket = defaultBucket;

        Path directory =
                Files.createDirectories(Path.of(tempDir));

        Path file =
                Files.createTempFile(
                        directory,
                        "moderation-",
                        ".video"
                );

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        var streamOptional =
                storageProvider.getObjectStream(
                        bucket,
                        event.getObjectKey()
                );

        if (streamOptional.isEmpty()) {
            deleteQuietly(file);

            throw new IOException(
                    "Video object not found: "
                            + bucket
                            + "/"
                            + event.getObjectKey()
            );
        }

        long copied = 0;

        try (
                InputStream source = streamOptional.get();

                DigestInputStream digestStream =
                        new DigestInputStream(source, digest);

                OutputStream target =
                        Files.newOutputStream(
                                file,
                                StandardOpenOption.TRUNCATE_EXISTING
                        )
        ) {

            byte[] buffer = new byte[1024 * 1024];

            int read;

            while ((read = digestStream.read(buffer)) != -1) {
                target.write(buffer, 0, read);
                copied += read;
            }

        } catch (Exception e) {

            deleteQuietly(file);
            throw e;
        }

        if (copied == 0) {
            deleteQuietly(file);

            throw new IOException(
                    "Downloaded video is empty"
            );
        }

        if (event.getFileSize() != null
                && event.getFileSize() > 0
                && event.getFileSize() != copied) {

            deleteQuietly(file);

            throw new IOException(
                    "Video size mismatch: expected="
                            + event.getFileSize()
                            + ", actual="
                            + copied
            );
        }

        String expected = event.getChecksum();

        if (expected != null && !expected.isBlank()) {

            String actual =
                    HexFormat.of().formatHex(
                            digest.digest()
                    );

            if (!expected.equalsIgnoreCase(actual)) {

                deleteQuietly(file);

                throw new IOException(
                        "Video SHA-256 checksum mismatch"
                );
            }
        }

        return file;
    }

    private void blockVideo(
            Video video,
            String details,
            double confidence) {

        video.block(
                SYSTEM_USER_ID,
                String.format(
                        "Automated moderation: confidence=%.3f; %s",
                        confidence,
                        details
                )
        );

        videoRepository.save(video);
    }

    private void blockVideoOnError(
            Video video,
            Exception error) {

        if (video.isBlocked()) {
            return;
        }

        video.block(
                SYSTEM_USER_ID,
                "Moderation system error: "
                        + safeMessage(error)
        );

        videoRepository.save(video);
    }

    private String safeMessage(Exception e) {

        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }

        return message.length() > 450
                ? message.substring(0, 450)
                : message;
    }

    private void deleteQuietly(Path file) {

        if (file == null) {
            return;
        }

        try {
            Files.deleteIfExists(file);

        } catch (IOException e) {

            log.warn(
                    "Could not delete moderation temp file {}: {}",
                    file,
                    e.getMessage()
            );
        }
    }
}
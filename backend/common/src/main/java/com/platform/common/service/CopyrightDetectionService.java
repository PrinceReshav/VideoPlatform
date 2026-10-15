package com.platform.common.service;

import com.platform.common.entity.Video;
import com.platform.common.event.UploadCompletedEvent;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.repository.VideoRepository;
import com.platform.common.storage.StorageProvider;
import com.platform.common.service.CopyrightClaimService;
import com.platform.common.entity.CopyrightClaim;
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
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Service for detecting copyrighted content in uploaded videos and automatically
 * generating copyright claims when infringement is detected (Feature 4: Copyright Detection & Claims System).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyrightDetectionService {

    private final VideoRepository videoRepository;
    private final StorageProvider storageProvider;
    private final CopyrightClaimService copyrightClaimService;

    @Value("${storage.minio.bucket-name:videos}")
    private String defaultBucket;

    @Value("${copyright.detection.enabled:true}")
    private boolean detectionEnabled;

    @Value("${copyright.detection.sensitivity:0.8}")
    private double detectionSensitivity;

    @Value("${copyright.detection.block-on-detection:true}")
    private boolean blockOnDetection;

    @Value("${copyright.detection.temp-dir:${java.io.tmpdir}/copyright-detection}")
    private String tempDir;

    // System user ID for automated actions
    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * Get whether copyright detection is enabled.
     */
    public boolean isDetectionEnabled() {
        return detectionEnabled;
    }

    /**
     * Set whether copyright detection is enabled.
     */
    public void setDetectionEnabled(boolean detectionEnabled) {
        this.detectionEnabled = detectionEnabled;
    }

    /**
     * Get copyright detection sensitivity.
     */
    public double getDetectionSensitivity() {
        return detectionSensitivity;
    }

    /**
     * Set copyright detection sensitivity.
     */
    public void setDetectionSensitivity(double detectionSensitivity) {
        this.detectionSensitivity = detectionSensitivity;
    }

    /**
     * Get whether to block videos when copyright is detected.
     */
    public boolean isBlockOnDetection() {
        return blockOnDetection;
    }

    /**
     * Set whether to block videos when copyright is detected.
     */
    public void setBlockOnDetection(boolean blockOnDetection) {
        this.blockOnDetection = blockOnDetection;
    }

    /**
     * Handle video uploaded events by checking for copyrighted content.
     */
    @KafkaListener(
            topics = KafkaTopicNames.VIDEO_UPLOADED,
            groupId = "${spring.kafka.consumer.group-id:copyright-detection-service}"
    )
    public void handleVideoUploaded(
            @Payload EventEnvelope<UploadCompletedEvent> envelope) {

        if (!detectionEnabled) {
            log.debug("Copyright detection is disabled");
            return;
        }

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
                    "Video {} is already blocked; copyright detection skipped",
                    videoId
            );
            return;
        }

        Path localFile = null;

        try {
            /*
             * Download the uploaded object locally and verify its
             * size/checksum before analyzing it.
             */
            localFile = downloadAndVerify(event);

            /*
             * Analyze the video for copyrighted content.
             * In a real implementation, this would use audio fingerprinting,
             * video hashing, and matching against copyright databases.
             * For this implementation, we'll use a simplified approach.
             */
            boolean copyrightDetected = analyzeForCopyright(localFile);

            if (copyrightDetected) {
                // Automatically generate a copyright claim
                createAutomaticCopyrightClaim(video, event);

                if (blockOnDetection) {
                    blockVideo(
                            video,
                            "Automatic copyright detection triggered claim generation"
                    );

                    log.warn(
                            "Video {} BLOCKED due to copyright detection",
                            videoId
                    );
                } else {
                    log.info(
                            "Video {} flagged for copyright review",
                            videoId
                    );
                }
            } else {
                log.info(
                        "Video {} passed copyright check",
                        videoId
                );
            }

        } catch (Exception e) {
            log.error(
                    "Copyright detection failed for video {}",
                    videoId,
                    e
            );

            if (blockOnDetection) {
                blockVideoOnError(video, e);
            }
        } finally {
            deleteQuietly(localFile);
        }
    }

    /**
     * Analyze a video file for copyrighted content.
     *
     * In a real implementation, this would:
     * 1. Extract audio and apply audio fingerprinting (e.g., AcoustID/Chromaprint)
     * 2. Extract video frames and apply perceptual hashing
     * 3. Match fingerprints/hashes against databases of known copyrighted works
     *
     * For this implementation, we'll use a simplified approach that checks:
     * - File name for known copyrighted titles
     * - File size against known ranges for movies/TV shows
     * - Simulated detection based on sensitivity setting
     */
    private boolean analyzeForCopyright(Path file) throws IOException {
        // In a real implementation, this would do actual copyright detection

        // For demonstration, we'll simulate detection based on:
        // 1. File name containing certain keywords
        // 2. Random chance based on sensitivity setting

        String fileName = file.getFileName().toString().toLowerCase();

        // Check for obvious copyright indicators in filename
        List<String> copyrightIndicators = List.of(
                "movie", "film", "cinema",
                "netflix", "hulu", "disney",
                "warner", "universal", "paramount",
                "sony", "fox", "mgm",
                "official", "trailer", "teaser",
                "hd", "4k", "bluray", "dvdrip"
        );

        boolean filenameMatch = copyrightIndicators.stream()
                .anyMatch(indicator -> fileName.contains(indicator));

        // Also check file size - movies tend to be large
        long fileSize;
        try {
            fileSize = Files.size(file);
        } catch (IOException e) {
            fileSize = 0;
        }

        boolean sizeIndicatesMovie = fileSize > 500 * 1024 * 1024; // > 500MB

        // Combine factors with some randomness for simulation
        double baseScore = 0.0;
        if (filenameMatch) baseScore += 0.4;
        if (sizeIndicatesMovie) baseScore += 0.3;

        // Add some variability based on file content (simulated)
        // In reality, this would come from actual analysis
        double variability = Math.random() * 0.3;
        double finalScore = Math.min(1.0, baseScore + variability);

        boolean detected = finalScore >= detectionSensitivity;

        if (detected) {
            log.info("Simulated copyright detection triggered for {} (score: {:.2f})",
                    file.getFileName(), finalScore);
        }

        return detected;
    }

    /**
     * Create an automatic copyright claim when infringement is detected.
     */
    private void createAutomaticCopyrightClaim(Video video, UploadCompletedEvent event) {
        // In a real implementation, we would:
        // 1. Identify the specific copyrighted work that was detected
        // 2. Determine the rights owner
        // 3. Provide details about the infringement

        // For this implementation, we'll create a placeholder claim

        CopyrightClaim claim = CopyrightClaim.builder()
                .videoId(video.getId())
                .claimantId(SYSTEM_USER_ID) // System-generated claim
                .rightsOwnerId(SYSTEM_USER_ID) // Placeholder - would be actual rights owner
                .copyrightedWorkDescription("Potentially copyrighted content detected via automated scanning")
                .copyrightRegistrationNumber("UNKNOWN")
                .originalWorkUrl("https://example.com/placeholder")
                .infringementTimestamp(0) // Would need more analysis to determine
                .infringementDuration((int) video.getDurationSeconds()) // Assume full duration for now
                .infringementType("UNKNOWN") // Would need analysis to determine type
                .status("PENDING")
                .actionTaken("NONE")
                .legallyValidated(false)
                .evidence("{\"detection_method\":\"automated_scan\",\"sensitivity\":\"\" + detectionSensitivity + \"\",\"trigger_reason\":\"filename_or_size_indicators\"}")
                .build();

        CopyrightClaim savedClaim = copyrightClaimService.createCopyrightClaim(claim);
        log.info("Generated automatic copyright claim {} for video {} due to copyright detection",
                savedClaim.getId(), video.getId());
    }

    /**
     * Block a video due to copyright detection.
     */
    private void blockVideo(
            Video video,
            String details) {
        video.block(
                SYSTEM_USER_ID,
                String.format(
                        "Automated copyright detection: %s",
                        details
                )
        );

        videoRepository.save(video);
    }

    /**
     * Block a video due to copyright detection error.
     */
    private void blockVideoOnError(
            Video video,
            Exception error) {
        if (video.isBlocked()) {
            return;
        }

        video.block(
                SYSTEM_USER_ID,
                "Copyright detection system error: "
                        + safeMessage(error)
        );

        videoRepository.save(video);
    }

    /**
     * Download and verify the uploaded video file.
     */
    private Path downloadAndVerify(
            UploadCompletedEvent event) throws Exception {

        String bucket = defaultBucket;

        Path directory =
                Files.createDirectories(Path.of(tempDir));

        Path file =
                Files.createTempFile(
                        directory,
                        "copyright_check-",
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
                    HexFormat.of().formatHex(digest.digest()).toLowerCase();

            if (!expected.equalsIgnoreCase(actual)) {
                deleteQuietly(file);

                throw new IOException(
                        "Video SHA-256 checksum mismatch"
                );
            }
        }

        return file;
    }

    /**
     * Safe error message extraction.
     */
    private String safeMessage(Exception e) {
        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }

        return message.length() > 450
                ? message.substring(0, 450)
                : message;
    }

    /**
     * Delete file quietly.
     */
    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn(
                    "Could not delete copyright detection temp file {}: {}",
                    file,
                    e.getMessage()
            );
        }
    }
}
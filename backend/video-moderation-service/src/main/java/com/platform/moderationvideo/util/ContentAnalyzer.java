package com.platform.moderationvideo.util;

import com.platform.moderationvideo.dto.FrameAnalysisResult;
import com.platform.moderationvideo.dto.VideoModerationResult;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_objdetect.HOGDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

/**
 * Deterministic OpenCV-based moderation analyzer.

 * This implementation uses Bytedeco OpenCV through JavaCV.

 * Important:
 * OpenCV heuristics are NOT a CNN/AI model. A real CNN/NSFW classifier
 * requires a trained model and an inference runtime such as ONNX Runtime.

 * This class therefore performs deterministic screening using:

 * - skin-color ratio
 * - optional Haar/LBP face detection
 * - optional HOG person detection
 * - optional perceptual-hash matching
 * - temporal score smoothing

 * It does not claim that these heuristics are an AI prediction.
 */
@Slf4j
@Component
public class ContentAnalyzer {

    @Value("${moderation.explicit-content-threshold:0.70}")
    private double explicitContentThreshold;

    @Value("${moderation.enable-face-detection:true}")
    private boolean enableFaceDetection;

    @Value("${moderation.enable-skin-color-detection:true}")
    private boolean enableSkinColorDetection;

    @Value("${moderation.enable-hog-person-detection:true}")
    private boolean enableHogPersonDetection;

    @Value("${moderation.skin-color-threshold:0.30}")
    private double skinColorThreshold;

    @Value("${moderation.face-count-threshold:3}")
    private int faceCountThreshold;

    @Value("${moderation.person-density-threshold:0.40}")
    private double personDensityThreshold;

    @Value("${moderation.temporal-window-size:10}")
    private int temporalWindowSize;

    @Value("${moderation.temporal-smoothing-alpha:0.30}")
    private double temporalSmoothingAlpha;

    @Value("${moderation.perceptual-hashing-enabled:true}")
    private boolean perceptualHashingEnabled;

    @Value("${moderation.known-bad-content-threshold:0.85}")
    private double knownBadContentThreshold;

    @Value("${moderation.known-bad-hashes-file:}")
    private String knownBadHashesFile;

    @Value("${moderation.face-cascade-path:}")
    private String faceCascadePath;

    @Getter
    private final AtomicLong framesProcessed = new AtomicLong();

    @Getter
    private final AtomicLong explicitContentDetected = new AtomicLong();

    @Getter
    private final AtomicLong knownContentDetected = new AtomicLong();

    private final HOGDescriptor hogDescriptor =
            new HOGDescriptor();

    private CascadeClassifier faceCascade;

    private final Set<String> knownBadHashes =
            new HashSet<>();

    @PostConstruct
    void initializeDetectors() {

        /*
         * HOG person detector.
         */
        if (enableHogPersonDetection) {
            try {
                Mat defaultPeopleDetector =
                        new Mat(
                                HOGDescriptor.getDefaultPeopleDetector(),
                                true
                        );

                try {
                    hogDescriptor.setSVMDetector(defaultPeopleDetector);
                    log.info("HOG person detector initialized");
                } finally {
                    defaultPeopleDetector.release();
                }

                log.info("HOG person detector initialized");

            } catch (Exception e) {
                log.warn(
                        "HOG person detector unavailable: {}",
                        e.getMessage()
                );
            }
        }

        /*
         * Optional face cascade.
         */
        if (enableFaceDetection
                && faceCascadePath != null
                && !faceCascadePath.isBlank()) {

            try {
                CascadeClassifier classifier =
                        new CascadeClassifier(faceCascadePath);

                if (classifier.empty()) {

                    classifier.close();

                    log.warn(
                            "Configured face cascade is not loadable: {}",
                            faceCascadePath
                    );

                } else {

                    faceCascade = classifier;

                    log.info(
                            "Face cascade initialized: {}",
                            faceCascadePath
                    );
                }

            } catch (Exception e) {

                faceCascade = null;

                log.warn(
                        "Face cascade initialization failed: {}",
                        e.getMessage()
                );
            }
        }
    }

    public VideoModerationResult analyzeFrames(
            List<Mat> frames,
            long fileSize) {

        if (frames == null || frames.isEmpty()) {
            throw new IllegalStateException("No analyzable video frames were extracted");
        }

        loadKnownBadHashesIfNecessary();

        framesProcessed.addAndGet(frames.size());

        List<FrameAnalysisResult> results =
                new ArrayList<>(frames.size());

        List<Double> scores =
                new ArrayList<>(frames.size());

        double maxScore = 0.0;

        boolean knownBad = false;

        for (int i = 0; i < frames.size(); i++) {

            Mat frame = frames.get(i);

            if (frame == null || frame.empty()) {
                continue;
            }

            FrameAnalysisResult result =
                    analyzeSingleFrame(frame, i);

            results.add(result);

            scores.add(
                    result.getExplicitnessScore()
            );

            maxScore =
                    Math.max(
                            maxScore,
                            result.getExplicitnessScore()
                    );

            if (result.isKnownBadContent()) {
                knownBad = true;
                break;
            }
        }

        /*
         * Known-bad hash match gets priority over heuristic scoring.
         */
        if (knownBad) {

            knownContentDetected.incrementAndGet();

            explicitContentDetected.incrementAndGet();

            return VideoModerationResult.explicit(
                    knownBadContentThreshold,
                    "Known-bad perceptual hash matched; "
                            + "fileSize="
                            + fileSize,
                    results
            );
        }

        /*
         * If every extracted frame was unusable.
         */
        if (scores.isEmpty()) {
            throw new IllegalStateException("No valid frames were available for moderation");
        }

        double smoothed =
                temporalScore(scores);

        boolean blocked =
                smoothed >= clamp(
                        explicitContentThreshold
                );

        if (blocked) {
            explicitContentDetected.incrementAndGet();
        }

        String details =
                String.format(
                        "Analyzed %d frames; "
                                + "maxScore=%.3f; "
                                + "temporalScore=%.3f; "
                                + "explicit=%s",
                        results.size(),
                        maxScore,
                        smoothed,
                        blocked
                );

        return VideoModerationResult.builder()
                .explicitContentDetected(blocked)
                .confidence(smoothed)
                .details(details)
                .frameResults(results)
                .build();
    }

    private FrameAnalysisResult analyzeSingleFrame(
            Mat frame,
            int index) {

        List<String> detections =
                new ArrayList<>();

        double skinRatio =
                enableSkinColorDetection
                        ? detectSkinRatio(frame)
                        : 0.0;

        int faceCount =
                enableFaceDetection
                        ? detectFaceCount(frame)
                        : 0;

        int personCount =
                enableHogPersonDetection
                        ? detectPersonCount(frame)
                        : 0;

        boolean knownBad =
                perceptualHashingEnabled
                        && checkPerceptualHash(frame);

        /*
         * Conservative heuristic score.
         *
         * Skin color alone is deliberately weak.
         */
        double score = 0.0;

        if (skinRatio >= skinColorThreshold) {

            score +=
                    0.35
                            * Math.min(
                            1.0,
                            skinRatio
                                    / Math.max(
                                    skinColorThreshold,
                                    0.01
                            )
                    );

            detections.add(
                    "high-skin-area"
            );
        }

        if (faceCount > 0) {

            detections.add(
                    "faces=" + faceCount
            );

            if (faceCount >= faceCountThreshold) {
                score += 0.10;
                detections.add(
                        "high-face-count"
                );
            }
        }

        if (personCount > 0) {

            detections.add(
                    "persons=" + personCount
            );

            double personDensitySignal =
                    (double) personCount
                            / Math.max(
                            1,
                            faceCountThreshold
                    );

            if (personDensitySignal
                    >= personDensityThreshold) {

                detections.add(
                        "high-person-density"
                );

                score += 0.10;
            }
        }

        if (knownBad) {
            detections.add(
                    "known-bad-hash"
            );
        }

        score = clamp(score);

        return FrameAnalysisResult.builder()
                .frameIndex(index)
                .explicitnessScore(score)
                .knownBadContent(knownBad)
                .skinRatio(skinRatio)
                .faceCount(faceCount)
                .personCount(personCount)
                .detections(detections)
                .build();
    }

    private double detectSkinRatio(Mat frame) {

        if (frame == null || frame.empty()) {
            return 0.0;
        }

        Mat hsv = new Mat();

        try {

            cvtColor(
                    frame,
                    hsv,
                    COLOR_BGR2HSV
            );

            if (hsv.empty()
                    || hsv.rows() <= 0
                    || hsv.cols() <= 0) {
                return 0.0;
            }

            UByteIndexer indexer =
                    hsv.createIndexer();

            try {

                long totalPixels =
                        (long) hsv.rows()
                                * hsv.cols();

                if (totalPixels <= 0) {
                    return 0.0;
                }

                long skinPixels = 0;

                for (int y = 0; y < hsv.rows(); y++) {

                    for (int x = 0; x < hsv.cols(); x++) {

                        int hue =
                                indexer.get(y, x, 0)
                                        & 0xFF;

                        int saturation =
                                indexer.get(y, x, 1)
                                        & 0xFF;

                        int value =
                                indexer.get(y, x, 2)
                                        & 0xFF;

                        /*
                         * OpenCV HSV ranges:
                         *
                         * H = 0..180
                         * S = 0..255
                         * V = 0..255
                         *
                         * Preserve the two existing skin-color
                         * ranges from the previous implementation:
                         *
                         * Range 1:
                         * H 0..25
                         * S 20..255
                         * V 40..255
                         *
                         * Range 2:
                         * H 160..180
                         * S 20..255
                         * V 40..255
                         */
                        boolean firstRange =
                                hue >= 0
                                        && hue <= 25
                                        && saturation >= 20
                                        && value >= 40;

                        boolean secondRange =
                                hue >= 160
                                        && hue <= 180
                                        && saturation >= 20
                                        && value >= 40;

                        if (firstRange || secondRange) {
                            skinPixels++;
                        }
                    }
                }

                return skinPixels
                        / (double) totalPixels;

            } finally {
                indexer.release();
            }

        } catch (Exception e) {

            log.debug(
                    "Skin analysis failed: {}",
                    e.getMessage()
            );

            return 0.0;

        } finally {
            hsv.release();
        }
    }

    private int detectFaceCount(Mat frame) {

        if (faceCascade == null
                || faceCascade.empty()) {

            return 0;
        }

        Mat gray = new Mat();
        RectVector faces =
                new RectVector();

        try {

            cvtColor(
                    frame,
                    gray,
                    COLOR_BGR2GRAY
            );

            equalizeHist(
                    gray,
                    gray
            );

            faceCascade.detectMultiScale(
                    gray,
                    faces,
                    1.1,
                    4,
                    0,
                    new Size(30, 30),
                    new Size()
            );

            return (int) faces.size();

        } catch (Exception e) {

            log.debug(
                    "Face analysis failed: {}",
                    e.getMessage()
            );

            return 0;

        } finally {

            gray.release();
            faces.close();
        }
    }

    private int detectPersonCount(Mat frame) {

        Mat resized = new Mat();
        RectVector found =
                new RectVector();

        try {

            int width = frame.cols();
            int height = frame.rows();

            if (width <= 0 || height <= 0) {
                return 0;
            }

            double scale =
                    Math.min(
                            1.0,
                            640.0
                                    / Math.max(
                                    width,
                                    height
                            )
                    );

            int resizedWidth =
                    Math.max(
                            1,
                            (int) Math.round(
                                    width * scale
                            )
                    );

            int resizedHeight =
                    Math.max(
                            1,
                            (int) Math.round(
                                    height * scale
                            )
                    );

            resize(
                    frame,
                    resized,
                    new Size(
                            resizedWidth,
                            resizedHeight
                    )
            );

            hogDescriptor.detectMultiScale(
                    resized,
                    found,
                    0.0,
                    new Size(8, 8),
                    new Size(32, 32),
                    1.05,
                    2.0,
                    false
            );

            return (int) found.size();

        } catch (Exception e) {

            log.debug(
                    "HOG analysis failed: {}",
                    e.getMessage()
            );

            return 0;

        } finally {

            resized.release();
            found.close();
        }
    }

    private double temporalScore(
            List<Double> scores) {

        if (scores == null
                || scores.isEmpty()) {

            return 0.0;
        }

        int window =
                Math.max(
                        1,
                        Math.min(
                                temporalWindowSize,
                                scores.size()
                        )
                );

        double alpha =
                clamp(
                        temporalSmoothingAlpha
                );

        double ema =
                clamp(scores.get(0));

        for (int i = 1;
             i < scores.size();
             i++) {

            double current =
                    clamp(scores.get(i));

            ema =
                    alpha * current
                            + (1.0 - alpha) * ema;
        }

        double recentAverage =
                scores.subList(
                                scores.size() - window,
                                scores.size()
                        )
                        .stream()
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .average()
                        .orElse(0.0);

        return clamp(
                (ema * 0.60)
                        + (recentAverage * 0.40)
        );
    }

    private boolean checkPerceptualHash(
            Mat frame) {

        if (knownBadHashes.isEmpty()) {
            return false;
        }

        Mat small = new Mat();
        Mat gray = new Mat();

        try {

            resize(
                    frame,
                    small,
                    new Size(32, 32)
            );

            cvtColor(
                    small,
                    gray,
                    COLOR_BGR2GRAY
            );

            resize(
                    gray,
                    small,
                    new Size(9, 8)
            );

            UByteIndexer indexer =
                    small.createIndexer();

            try {

                StringBuilder hash =
                        new StringBuilder(64);

                for (int y = 0; y < 8; y++) {

                    for (int x = 0; x < 8; x++) {

                        int left =
                                indexer.get(y, x)
                                        & 0xFF;

                        int right =
                                indexer.get(y, x + 1)
                                        & 0xFF;

                        hash.append(
                                left > right
                                        ? '1'
                                        : '0'
                        );
                    }
                }

                String value =
                        hash.toString();

                for (String known :
                        knownBadHashes) {

                    if (hammingDistance(
                            value,
                            known
                    ) <= 6) {

                        return true;
                    }
                }

            } finally {

                indexer.release();
            }

        } catch (Exception e) {

            log.debug(
                    "Perceptual hash failed: {}",
                    e.getMessage()
            );

        } finally {

            small.release();
            gray.release();
        }

        return false;
    }

    private int hammingDistance(
            String a,
            String b) {

        if (a == null
                || b == null
                || a.length() != b.length()) {

            return Integer.MAX_VALUE;
        }

        int distance = 0;

        for (int i = 0;
             i < a.length();
             i++) {

            if (a.charAt(i)
                    != b.charAt(i)) {

                distance++;
            }
        }

        return distance;
    }

    private void loadKnownBadHashesIfNecessary() {

        if (!knownBadHashes.isEmpty()) {
            return;
        }

        if (knownBadHashesFile == null
                || knownBadHashesFile.isBlank()) {

            return;
        }

        Path path =
                Paths.get(knownBadHashesFile);

        if (!Files.isRegularFile(path)) {

            log.warn(
                    "Known-bad hash file not found: {}",
                    path
            );

            return;
        }

        try (
                BufferedReader reader =
                        Files.newBufferedReader(
                                path,
                                StandardCharsets.UTF_8
                        )
        ) {

            String line;

            while ((line = reader.readLine())
                    != null) {

                String normalized =
                        line.trim()
                                .toLowerCase();

                if (normalized.matches(
                        "[01]{64}"
                )) {

                    knownBadHashes.add(
                            normalized
                    );
                }
            }

            log.info(
                    "Loaded {} known-bad perceptual hashes",
                    knownBadHashes.size()
            );

        } catch (IOException e) {

            log.warn(
                    "Could not load known-bad hashes: {}",
                    e.getMessage()
            );
        }
    }

    private double clamp(double value) {

        if (!Double.isFinite(value)) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(1.0, value)
        );
    }

    public void resetStatistics() {

        framesProcessed.set(0);
        explicitContentDetected.set(0);
        knownContentDetected.set(0);
    }

    public double getDetectionRate() {

        long processed =
                framesProcessed.get();

        if (processed == 0) {
            return 0.0;
        }

        return explicitContentDetected.get()
                * 100.0
                / processed;
    }

    public double getKnownContentDetectionRate() {

        long processed =
                framesProcessed.get();

        if (processed == 0) {
            return 0.0;
        }

        return knownContentDetected.get()
                * 100.0
                / processed;
    }
}
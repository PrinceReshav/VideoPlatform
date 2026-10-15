package com.platform.moderationvideo.util;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts a bounded number of video frames using the FFmpeg binaries
 * bundled by JavaCV.
 */
@Component
public class FfmpegFrameExtractor {

    private static final Logger log =
            LoggerFactory.getLogger(FfmpegFrameExtractor.class);

    private final OpenCVFrameConverter.ToMat converter =
            new OpenCVFrameConverter.ToMat();

    public List<Mat> extractFrames(
            File videoFile,
            double samplingFps,
            int maxFrames) throws Exception {

        if (videoFile == null || !videoFile.isFile()) {
            throw new IllegalArgumentException(
                    "Video file does not exist: " + videoFile);
        }

        if (samplingFps <= 0.0 || !Double.isFinite(samplingFps)) {
            throw new IllegalArgumentException(
                    "samplingFps must be > 0");
        }

        if (maxFrames <= 0) {
            throw new IllegalArgumentException(
                    "maxFrames must be > 0");
        }

        List<Mat> frames =
                new ArrayList<>(Math.min(maxFrames, 256));

        try (FFmpegFrameGrabber grabber =
                     new FFmpegFrameGrabber(videoFile)) {

            grabber.setOption("threads", "1");
            grabber.start();

            double sourceFps = grabber.getFrameRate();

            if (!Double.isFinite(sourceFps) || sourceFps <= 0.0) {
                sourceFps = 25.0;
            }

            int interval = Math.max(
                    1,
                    (int) Math.round(sourceFps / samplingFps)
            );

            long totalFrames = grabber.getLengthInFrames();

            log.info(
                    "Moderation video: fps={}, frames={}, duration={}s, " +
                            "sampleEvery={} frames, maxSamples={}",
                    sourceFps,
                    totalFrames,
                    totalFrames > 0
                            ? totalFrames / sourceFps
                            : -1,
                    interval,
                    maxFrames
            );

            long frameNumber = 0;

            while (frames.size() < maxFrames) {

                Frame frame = grabber.grabImage();

                if (frame == null) {
                    break;
                }

                if (frameNumber % interval == 0
                        && frame.image != null) {

                    Mat mat = converter.convert(frame);

                    if (mat != null && !mat.empty()) {
                        frames.add(mat.clone());
                    }
                }

                frameNumber++;
            }

        } catch (Exception e) {
            release(frames);
            throw e;
        }

        log.info(
                "Extracted {} moderation frames from {}",
                frames.size(),
                videoFile.getName()
        );

        return frames;
    }

    public void release(List<Mat> frames) {

        if (frames == null) {
            return;
        }

        for (Mat frame : frames) {
            if (frame != null) {
                frame.release();
            }
        }

        frames.clear();
    }
}
package com.platform.moderationvideo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrameAnalysisResult {

    /**
     * Zero-based index of the sampled frame.
     */
    private int frameIndex;

    /**
     * Heuristic explicitness score between 0.0 and 1.0.
     */
    private double explicitnessScore;

    /**
     * Whether the frame matched a configured known-bad perceptual hash.
     */
    private boolean knownBadContent;

    /**
     * Approximate skin-colored pixel ratio in the frame.
     */
    private double skinRatio;

    /**
     * Number of detected faces.
     */
    private int faceCount;

    /**
     * Number of detected people from the HOG detector.
     */
    private int personCount;

    /**
     * Human-readable detector signals generated for this frame.
     */
    private List<String> detections;
}
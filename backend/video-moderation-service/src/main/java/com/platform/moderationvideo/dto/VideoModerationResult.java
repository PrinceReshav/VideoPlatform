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
public class VideoModerationResult {

    private boolean explicitContentDetected;

    private double confidence;

    private String details;

    private List<FrameAnalysisResult> frameResults;

    public static VideoModerationResult clean(String details) {
        return builder()
                .explicitContentDetected(false)
                .confidence(0.0)
                .details(details)
                .frameResults(List.of())
                .build();
    }

    public static VideoModerationResult explicit(
            double confidence,
            String details) {

        return builder()
                .explicitContentDetected(true)
                .confidence(clamp(confidence))
                .details(details)
                .frameResults(List.of())
                .build();
    }

    public static VideoModerationResult explicit(
            double confidence,
            String details,
            List<FrameAnalysisResult> frameResults) {

        return builder()
                .explicitContentDetected(true)
                .confidence(clamp(confidence))
                .details(details)
                .frameResults(
                        frameResults == null
                                ? List.of()
                                : List.copyOf(frameResults)
                )
                .build();
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(1.0, value)
        );
    }
}
package com.platform.transcoding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionLadder {
    private int sourceWidth;
    private int sourceHeight;
    private List<Resolution> resolutions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resolution {
        private String name; // e.g., "144p", "720p"
        private int width;
        private int height;
        private int bitrateKbps;
        private String profile; // baseline, main, high
    }

    public static ResolutionLadder defaultLadder(int sourceWidth, int sourceHeight) {
        List<Resolution> allResolutions = List.of(
                Resolution.builder().name("144p").width(256).height(144).bitrateKbps(100).profile("baseline").build(),
                Resolution.builder().name("240p").width(426).height(240).bitrateKbps(200).profile("baseline").build(),
                Resolution.builder().name("360p").width(640).height(360).bitrateKbps(400).profile("baseline").build(),
                Resolution.builder().name("480p").width(854).height(480).bitrateKbps(800).profile("main").build(),
                Resolution.builder().name("720p").width(1280).height(720).bitrateKbps(2000).profile("high").build(),
                Resolution.builder().name("1080p").width(1920).height(1080).bitrateKbps(5000).profile("high").build(),
                Resolution.builder().name("1440p").width(2560).height(1440).bitrateKbps(10000).profile("high").build(),
                Resolution.builder().name("2160p").width(3840).height(2160).bitrateKbps(20000).profile("high").build()
        );

        // Filter to only include resolutions <= source resolution
        List<Resolution> filtered = allResolutions.stream()
                .filter(r -> r.getHeight() <= sourceHeight)
                .toList();

        return ResolutionLadder.builder()
                .sourceWidth(sourceWidth)
                .sourceHeight(sourceHeight)
                .resolutions(filtered)
                .build();
    }
}
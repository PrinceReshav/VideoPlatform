package com.platform.livetv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.livetv.entity.LiveChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional official YouTube Live fallback. It never extracts or proxies a
 * YouTube media URL; playback remains inside YouTube's official iframe player.
 */
@Service
public class YouTubeLiveFallbackService {
    private final ObjectMapper objectMapper;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    @Value("${livetv.youtube.enabled:false}")
    private boolean enabled;
    @Value("${livetv.youtube.api-key:}")
    private String apiKey;
    @Value("${livetv.youtube.cache-seconds:300}")
    private long cacheSeconds;
    @Value("${livetv.youtube.max-results:10}")
    private int maxResults;

    public YouTubeLiveFallbackService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FallbackResponse findLive(LiveChannel channel) {
        if (channel == null) return FallbackResponse.disabled("Channel is missing");
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return FallbackResponse.disabled("YouTube Live fallback is not configured");
        }
        String cacheKey = (channel.getTvgId() == null ? channel.getName() : channel.getTvgId()).trim().toLowerCase(Locale.ROOT);
        CachedResult cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.response();

        try {
            String query = channel.getName() == null ? "" : channel.getName().trim();
            if (query.isBlank()) return FallbackResponse.notFound("Channel has no searchable name");
            StringBuilder url = new StringBuilder("https://www.googleapis.com/youtube/v3/search?")
                    .append("part=snippet&type=video&eventType=live")
                    .append("&videoEmbeddable=true&videoSyndicated=true")
                    .append("&maxResults=").append(Math.max(1, Math.min(25, maxResults)))
                    .append("&q=").append(URLEncoder.encode(query + " live", StandardCharsets.UTF_8))
                    .append("&key=").append(URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8));
            if (channel.getCountryCode() != null && channel.getCountryCode().matches("[A-Za-z]{2}")) {
                url.append("&regionCode=").append(channel.getCountryCode().toUpperCase(Locale.ROOT));
            }
            JsonNode search = getJson(url.toString());
            List<YouTubeCandidate> candidates = new ArrayList<>();
            for (JsonNode item : search.path("items")) {
                String videoId = item.path("id").path("videoId").asText("");
                if (videoId.isBlank()) continue;
                String title = item.path("snippet").path("title").asText("");
                String channelTitle = item.path("snippet").path("channelTitle").asText("");
                String published = item.path("snippet").path("publishedAt").asText("");
                double score = similarity(query, title, channelTitle);
                candidates.add(new YouTubeCandidate(videoId, title, channelTitle, published, score));
            }
            candidates.sort(Comparator.comparingDouble(YouTubeCandidate::score).reversed());
            YouTubeCandidate best = null;
            int validated = 0;
            for (YouTubeCandidate candidate : candidates) {
                if (candidate.score() < 0.25d || validated >= 5) break;
                validated++;
                if (validateLiveVideo(candidate.videoId())) { best = candidate; break; }
            }
            FallbackResponse response = best == null
                    ? FallbackResponse.notFound("No embeddable live YouTube broadcast matched this channel")
                    : FallbackResponse.found(best.videoId(), best.title(), best.channelTitle(), best.score());
            cache.put(cacheKey, new CachedResult(response, Instant.now().plusSeconds(Math.max(30, cacheSeconds))));
            return response;
        } catch (Exception e) {
            FallbackResponse response = FallbackResponse.error(e.getMessage() == null ? "YouTube lookup failed" : e.getMessage());
            cache.put(cacheKey, new CachedResult(response, Instant.now().plusSeconds(Math.max(30, Math.min(60, cacheSeconds)))));
            return response;
        }
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "VideoPlatform-LiveTV/10")
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("YouTube API HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }


    private boolean validateLiveVideo(String videoId) throws Exception {
        String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet,status,liveStreamingDetails&id="
                + URLEncoder.encode(videoId, StandardCharsets.UTF_8)
                + "&key=" + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8);
        JsonNode root = getJson(url);
        JsonNode item = root.path("items").isArray() && root.path("items").size() > 0 ? root.path("items").get(0) : null;
        if (item == null) return false;
        return "live".equalsIgnoreCase(item.path("snippet").path("liveBroadcastContent").asText(""))
                && item.path("status").path("embeddable").asBoolean(false)
                && item.path("status").path("syndicated").asBoolean(false);
    }
    private double similarity(String query, String title, String channelTitle) {
        String q = normalize(query);
        String a = normalize(title);
        String b = normalize(channelTitle);
        if (a.contains(q) || b.contains(q)) return 1.0;
        String[] tokens = q.split(" ");
        int hit = 0;
        for (String token : tokens) if (token.length() >= 3 && (a.contains(token) || b.contains(token))) hit++;
        return tokens.length == 0 ? 0 : (double) hit / tokens.length;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public record YouTubeCandidate(String videoId, String title, String channelTitle, String publishedAt, double score) {}
    public record FallbackResponse(boolean enabled, boolean found, String videoId, String title, String channelTitle, double matchScore, String reason) {
        static FallbackResponse disabled(String reason) { return new FallbackResponse(false, false, "", "", "", 0, reason); }
        static FallbackResponse notFound(String reason) { return new FallbackResponse(true, false, "", "", "", 0, reason); }
        static FallbackResponse found(String id, String title, String channel, double score) { return new FallbackResponse(true, true, id, title, channel, score, ""); }
        static FallbackResponse error(String reason) { return new FallbackResponse(true, false, "", "", "", 0, reason); }
    }
    private record CachedResult(FallbackResponse response, Instant expiresAt) {}
}

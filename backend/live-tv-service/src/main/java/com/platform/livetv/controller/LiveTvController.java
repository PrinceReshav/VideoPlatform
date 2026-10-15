package com.platform.livetv.controller;

import com.platform.livetv.dto.CategoryStats;
import com.platform.livetv.dto.CountryStats;
import com.platform.livetv.entity.LiveChannel;
import com.platform.livetv.entity.EpgProgram;
import com.platform.livetv.repository.EpgProgramRepository;
import com.platform.livetv.service.IptvOrgSyncService;
import com.platform.livetv.service.YouTubeLiveFallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/live")
@Tag(
        name = "Live TV",
        description = "Live TV channels from IPTV-org with EPG and health monitoring"
)
public class LiveTvController {



    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 50;

    /*
     * Keep this list aligned with the fields exposed by LiveChannel and supported
     * by IptvOrgSyncService. The frontend currently uses these four sort fields.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "country",
            "category",
            "status"
    );

    private final IptvOrgSyncService liveTvService;
    private final YouTubeLiveFallbackService youTubeLiveFallbackService;
    private final EpgProgramRepository epgProgramRepository;

    public LiveTvController(
            IptvOrgSyncService liveTvService,
            YouTubeLiveFallbackService youTubeLiveFallbackService,
            EpgProgramRepository epgProgramRepository) {

        this.liveTvService = liveTvService;
        this.youTubeLiveFallbackService = youTubeLiveFallbackService;
        this.epgProgramRepository = epgProgramRepository;
    }

    @GetMapping("/readiness")
    @Operation(summary = "Get Live TV catalogue readiness")
    public ResponseEntity<Map<String, Object>> getReadiness() {
        return ResponseEntity.ok(liveTvService.getReadiness());
    }

    @GetMapping("/channels")
    @Operation(summary = "Get live TV channels with filtering")
    public ResponseEntity<Page<LiveChannel>> getChannels(
            @Parameter(description = "Country code filter (e.g., US, GB, IN)")
            @RequestParam(name = "country", required = false)
            String country,

            @Parameter(description = "Category filter (e.g., News, Sports, Entertainment)")
            @RequestParam(name = "category", required = false)
            String category,

            @Parameter(description = "Search query")
            @RequestParam(name = "search", required = false)
            String search,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(name = "page", defaultValue = "0")
            int page,

            @Parameter(description = "Page size, maximum 50")
            @RequestParam(name = "size", defaultValue = "50")
            int size,

            @Parameter(description = "Sort field with optional direction, e.g. name,asc or name,desc")
            @RequestParam(name = "sort", defaultValue = "name,asc")
            String sort) {

        final int safePage = normalizePage(page);
        final int safeSize = normalizePageSize(size);
        final String safeSort = normalizeSort(sort);

        final Page<LiveChannel> channels = liveTvService.getChannels(
                normalizeFilter(country),
                normalizeFilter(category),
                normalizeFilter(search),
                safePage,
                safeSize,
                safeSort
        );

        return ResponseEntity.ok(channels);
    }

    @GetMapping("/countries")
    @Operation(summary = "Get countries with counts for the active category/search filters")
    public ResponseEntity<List<CountryStats>> getCountries(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(
                liveTvService.getCountries(
                        normalizeFilter(category),
                        normalizeFilter(search)
                )
        );
    }

    @GetMapping("/categories")
    @Operation(summary = "Get categories with counts for the active country/search filters")
    public ResponseEntity<List<CategoryStats>> getCategories(
            @RequestParam(name = "country", required = false) String country,
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(
                liveTvService.getCategories(
                        normalizeFilter(country),
                        normalizeFilter(search)
                )
        );
    }

    @GetMapping("/channels/{id}")
    @Operation(summary = "Get a single channel by ID")
    public ResponseEntity<LiveChannel> getChannel(
            @PathVariable("id") UUID id) {

        Optional<LiveChannel> channel = liveTvService.getChannel(id);
        return channel
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/channels/tvg/{tvgId}")
    @Operation(summary = "Get a channel by TVG ID")
    public ResponseEntity<LiveChannel> getChannelByTvgId(
            @PathVariable("tvgId") String tvgId) {

        Optional<LiveChannel> channel =
                liveTvService.getChannelByTvgId(tvgId);

        return channel
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/channels/{id}/favorite")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Toggle favorite status for a channel")
    public ResponseEntity<LiveChannel> toggleFavorite(
            @PathVariable("id") UUID id) {

        return ResponseEntity.ok(liveTvService.toggleFavorite(id));
    }

    @PostMapping("/channels/{id}/view")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Record a view for a channel")
    public ResponseEntity<LiveChannel> recordView(
            @PathVariable("id") UUID id) {

        return ResponseEntity.ok(liveTvService.recordView(id));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured/popular channels")
    public ResponseEntity<List<LiveChannel>> getFeatured() {
        return ResponseEntity.ok(liveTvService.getFeatured());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleUpstreamFailure(IllegalStateException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "LIVE_STREAM_UPSTREAM_UNAVAILABLE",
                        "message", ex.getMessage() == null ? "Live stream source unavailable" : ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadLiveRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "INVALID_LIVE_STREAM_REQUEST",
                        "message", ex.getMessage() == null ? "Invalid live stream request" : ex.getMessage()
                ));
    }

    @GetMapping("/channels/{id}/stream-candidates")
    @Operation(summary = "Get all indexed IPTV stream candidates for a channel")
    public ResponseEntity<List<IptvOrgSyncService.StreamCandidateInfo>> streamCandidates(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(liveTvService.getStreamCandidateInfo(id));
    }

    @GetMapping("/channels/{id}/youtube-fallback")
    @Operation(summary = "Find an official embeddable YouTube Live fallback")
    public ResponseEntity<YouTubeLiveFallbackService.FallbackResponse> youtubeFallback(@PathVariable("id") UUID id) {
        LiveChannel channel = liveTvService.getChannel(id).orElseThrow(() -> new IllegalArgumentException("Live channel not found"));
        return ResponseEntity.ok(youTubeLiveFallbackService.findLive(channel));
    }

    @GetMapping("/channels/{id}/playback-diagnostic")
    @Operation(summary = "Diagnose live playback and classify the result")
    public ResponseEntity<IptvOrgSyncService.PlaybackDiagnostic> playbackDiagnostic(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(liveTvService.getPlaybackDiagnostic(id));
    }

    @GetMapping("/channels/{id}/stream.m3u8")
    @Operation(summary = "Proxy a live HLS stream through the Live TV service")
    public ResponseEntity<byte[]> proxyStream(
            @PathVariable("id") UUID id,
            @RequestParam(name = "url", required = false) String url) {

        IptvOrgSyncService.StreamProxyResponse proxied = liveTvService.proxyStream(id, url);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(proxied.contentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noStore())
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "*")
                .body(proxied.body());
    }

    @GetMapping("/channels/{id}/logo")
    @Operation(summary = "Serve an IPTV-org channel logo through the Live TV service")
    public ResponseEntity<byte[]> getLogo(@PathVariable("id") UUID id) {
        IptvOrgSyncService.LogoResponse logo = liveTvService.getLogo(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(logo.contentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(12)).cachePublic())
                .header("Access-Control-Allow-Origin", "*")
                .body(logo.body());
    }

    @GetMapping("/channels/{id}/epg")
    @Operation(summary = "Get EPG data for a channel")
    public ResponseEntity<Map<String, Object>> getEpg(
            @PathVariable("id") UUID id,
            @RequestParam(name = "date", required = false) String date) {

        LocalDate day;
        try {
            day = date == null || date.isBlank() ? LocalDate.now(ZoneOffset.UTC) : LocalDate.parse(date);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("date must use ISO-8601 format yyyy-MM-dd");
        }
        Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<EpgProgram> programs = epgProgramRepository.findForWindow(id, start, end);
        return ResponseEntity.ok(Map.of(
                "channelId", id.toString(),
                "date", day.toString(),
                "programs", programs.stream().map(p -> Map.of(
                        "id", p.getId(),
                        "title", p.getTitle(),
                        "description", p.getDescription() == null ? "" : p.getDescription(),
                        "startTime", p.getStartTime(),
                        "endTime", p.getEndTime(),
                        "category", p.getCategory() == null ? "" : p.getCategory(),
                        "icon", p.getIcon() == null ? "" : p.getIcon(),
                        "season", p.getSeason() == null ? 0 : p.getSeason(),
                        "episode", p.getEpisode() == null ? 0 : p.getEpisode(),
                        "episodeTitle", p.getEpisodeTitle() == null ? "" : p.getEpisodeTitle()
                )).toList()
        ));
    }

    @PostMapping("/channels/{id}/check")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger health check for a channel")
    public ResponseEntity<LiveChannel> checkChannel(
            @PathVariable("id") UUID id) {

        return ResponseEntity.ok(liveTvService.checkChannel(id));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger IPTV-org sync (admin only)")
    public ResponseEntity<String> triggerSync() {
        liveTvService.syncChannels();
        return ResponseEntity.accepted().body("Sync completed");
    }

    @PostMapping("/health-check")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger health check")
    public ResponseEntity<String> triggerHealthCheck() {
        liveTvService.healthCheckChannels();
        return ResponseEntity.accepted().body("Health check started");
    }

    private static int normalizePage(int page) {
        return Math.max(DEFAULT_PAGE, page);
    }

    private static int normalizePageSize(int size) {
        return Math.min(
                Math.max(1, size),
                MAX_PAGE_SIZE
        );
    }

    /**
     * Converts blank/null filters to null so the service/repository can treat
     * them as "no filter". Whitespace is removed from the beginning/end only.
     */
    private static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Accepts Spring-style sorting:
     *   name
     *   name,asc
     *   name,desc
     *
     * Only fields exposed by the Live TV API are accepted.
     */
    private static String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "name,asc";
        }

        String normalized = sort.trim();
        String[] parts = normalized.split(",");

        String field = parts[0].trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            return "name,asc";
        }

        String direction = "asc";

        if (parts.length >= 2 && !parts[1].isBlank()) {
            String requestedDirection =
                    parts[1].trim().toLowerCase(Locale.ROOT);

            if ("desc".equals(requestedDirection)) {
                direction = "desc";
            } else if (!"asc".equals(requestedDirection)) {
                return "name,asc";
            }
        }

        return field + "," + direction;
    }
}
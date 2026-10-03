package com.platform.livetv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.platform.livetv.dto.CategoryStats;
import com.platform.livetv.dto.CountryStats;
import com.platform.livetv.entity.LiveChannel;
import com.platform.livetv.repository.LiveChannelRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class IptvOrgSyncService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_HEALTH_BATCH_SIZE = 50;
    private static final int DEFAULT_FEATURED_SIZE = 20;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "name",
            "country",
            "countryCode",
            "category",
            "language",
            "status",
            "viewCount",
            "lastChecked"
    );

    private final LiveChannelRepository channelRepository;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final CacheManager cacheManager;

    @Value("${livetv.iptv-org.channels-url:https://iptv-org.github.io/api/channels.json}")
    private String channelsUrl;

    @Value("${livetv.iptv-org.streams-url:https://iptv-org.github.io/api/streams.json}")
    private String streamsUrl;

    @Value("${livetv.iptv-org.m3u-url:https://iptv-org.github.io/iptv/index.m3u}")
    private String m3uUrl;

    @Value("${livetv.iptv-org.feeds-url:https://iptv-org.github.io/api/feeds.json}")
    private String feedsUrl;

    @Value("${livetv.iptv-org.logos-url:https://iptv-org.github.io/api/logos.json}")
    private String logosUrl;

    @Value("${livetv.iptv-org.categories-url:https://iptv-org.github.io/api/categories.json}")
    private String categoriesUrl;

    @Value("${livetv.iptv-org.countries-url:https://iptv-org.github.io/api/countries.json}")
    private String countriesUrl;

    @Value("${livetv.iptv-org.languages-url:https://iptv-org.github.io/api/languages.json}")
    private String languagesUrl;

    @Value("${livetv.iptv-org.epg-url:https://raw.githubusercontent.com/iptv-org/epg/master/epg.xml.gz}")
    private String epgUrl;

    @Value("${livetv.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${livetv.health-check.enabled:true}")
    private boolean healthCheckEnabled;

    @Value("${livetv.health-check.timeout-seconds:10}")
    private int healthCheckTimeoutSeconds;

    @Value("${livetv.health-check.max-concurrent:50}")
    private int maxConcurrentChecks;

    private final Map<String, String> countryCodeToName = new ConcurrentHashMap<>();
    private final Map<String, String> categoryIdToName = new ConcurrentHashMap<>();
    private final Map<String, String> languageCodeToName = new ConcurrentHashMap<>();
    private final Map<String, String> channelToLanguage = new ConcurrentHashMap<>();
    private final Map<String, String> channelToLogo = new ConcurrentHashMap<>();

    public IptvOrgSyncService(
            LiveChannelRepository channelRepository,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper,
            @Qualifier("xmlMapper") XmlMapper xmlMapper,
            CacheManager cacheManager) {
        this.channelRepository = channelRepository;
        this.objectMapper = objectMapper;
        this.xmlMapper = xmlMapper;
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void init() {
        if (!syncEnabled) {
            log.info("IPTV-org startup sync is disabled.");
            return;
        }

        Thread startupThread = new Thread(() -> {
            try {
                syncChannels();
            } catch (Exception e) {
                log.error("Initial IPTV-org sync failed", e);
            }
        }, "iptv-org-initial-sync");

        startupThread.setDaemon(true);
        startupThread.start();
    }

    @Scheduled(
            initialDelayString = "${livetv.sync.initial-delay-ms:21600000}",
            fixedRateString = "${livetv.sync.interval-ms:21600000}"
    )
    public void scheduledSync() {
        if (!syncEnabled) {
            log.debug("Scheduled IPTV-org sync skipped because synchronization is disabled.");
            return;
        }

        syncChannels();
    }

    /**
     * Synchronizes channel metadata and stream URLs from IPTV-org.
     *
     * A synchronization only persists channels for which a usable stream URL
     * was discovered. Existing records missing from the current usable stream
     * set are soft-deleted.
     */
    public synchronized void syncChannels() {
        if (!syncEnabled) {
            log.info("IPTV-org sync is disabled.");
            return;
        }

        long started = System.nanoTime();
        log.info("Starting IPTV-org Live TV sync...");

        try {
            loadReferenceData();

            JsonNode channels = readArray(channelsUrl);
            JsonNode feeds = readArray(feedsUrl);
            JsonNode logos = readArray(logosUrl);

            Map<String, List<JsonNode>> streamsByChannel = new HashMap<>();
            try {
                streamsByChannel = groupBy(readArray(streamsUrl), "channel");
            } catch (Exception e) {
                log.warn(
                        "Unable to load IPTV-org streams.json; falling back to M3U: {}",
                        e.getMessage()
                );
            }

            Map<String, List<M3uEntry>> m3uByChannel = new HashMap<>();
            if (streamsByChannel.isEmpty()) {
                try {
                    m3uByChannel = parseM3u(fetchUrl(m3uUrl));
                } catch (Exception e) {
                    log.warn(
                            "Unable to load IPTV-org M3U fallback: {}",
                            e.getMessage()
                    );
                }
            }

            buildFeedLanguageMap(feeds);
            buildLogoMap(logos);

            List<LiveChannel> existing = channelRepository.findAllByDeletedFalse();

            Map<String, LiveChannel> existingByTvgId = new HashMap<>();
            for (LiveChannel channel : existing) {
                String tvgId = channel.getTvgId();
                if (tvgId != null && !tvgId.isBlank()) {
                    existingByTvgId.put(tvgId, channel);
                }
            }

            List<LiveChannel> channelsToSave = new ArrayList<>();
            Set<String> seenTvgIds = new HashSet<>();

            int skippedNoStream = 0;
            int skippedInvalid = 0;

            for (JsonNode channelNode : channels) {
                try {
                    String tvgId = text(channelNode, "id");
                    String name = text(channelNode, "name");
                    String rawCountryCode = text(channelNode, "country");
                    String countryCode = rawCountryCode == null
                            ? ""
                            : rawCountryCode.trim().toUpperCase(Locale.ROOT);

                    if (tvgId == null || tvgId.isBlank()
                            || name == null || name.isBlank()
                            || countryCode.isBlank()) {
                        skippedInvalid++;
                        continue;
                    }

                    String streamUrl = null;

                    JsonNode bestStream = chooseBestStream(
                            streamsByChannel.getOrDefault(tvgId, List.of())
                    );

                    if (bestStream != null) {
                        streamUrl = text(bestStream, "url");
                    }

                    if (streamUrl == null || streamUrl.isBlank()) {
                        M3uEntry bestEntry = chooseBestM3uEntry(
                                m3uByChannel.getOrDefault(tvgId, List.of())
                        );

                        if (bestEntry != null) {
                            streamUrl = bestEntry.url();
                        }
                    }

                    streamUrl = normalizeUrl(streamUrl);

                    if (streamUrl == null) {
                        skippedNoStream++;
                        continue;
                    }

                    LiveChannel channel = existingByTvgId.getOrDefault(
                            tvgId,
                            new LiveChannel()
                    );

                    channel.setTvgId(tvgId.trim());
                    channel.setName(name.trim());
                    channel.setCountryCode(countryCode);
                    channel.setCountry(
                            countryCodeToName.getOrDefault(countryCode, countryCode)
                    );
                    channel.setCategory(
                            resolvePrimaryCategory(channelNode.path("categories"))
                    );
                    channel.setLanguage(
                            channelToLanguage.getOrDefault(tvgId, "Unknown")
                    );

                    String logoUrl = normalizeUrl(channelToLogo.get(tvgId));
                    channel.setLogo(logoUrl);
                    channel.setTvgName(name.trim());
                    channel.setTvgLogo(logoUrl);
                    channel.setGroupTitle(channel.getCategory());
                    channel.setUrl(streamUrl);
                    channel.setHlsUrl(isHls(streamUrl) ? streamUrl : null);
                    channel.setNsfw(channelNode.path("is_nsfw").asBoolean(false));
                    channel.setDeleted(false);
                    channel.setDeletedAt(null);
                    channel.setDeletedBy(null);

                    if (channel.getStatus() == null) {
                        channel.setStatus(LiveChannel.StreamStatus.UNKNOWN);
                    }

                    channelsToSave.add(channel);
                    seenTvgIds.add(tvgId.trim());

                } catch (Exception itemError) {
                    skippedInvalid++;
                    log.debug("Skipping malformed IPTV-org channel", itemError);
                }
            }

            if (!channelsToSave.isEmpty()) {
                channelRepository.saveAll(channelsToSave);
            }

            int softDeleted = softDeleteMissingChannels(existing, seenTvgIds);

            clearCaches();

            long elapsedMs = Duration.ofNanos(
                    System.nanoTime() - started
            ).toMillis();

            log.info(
                    "IPTV-org sync completed: saved={}, softDeleted={}, skippedNoStream={}, skippedInvalid={}, elapsedMs={}",
                    channelsToSave.size(),
                    softDeleted,
                    skippedNoStream,
                    skippedInvalid,
                    elapsedMs
            );

        } catch (Exception e) {
            log.error("IPTV-org sync failed", e);
        }
    }

    private int softDeleteMissingChannels(
            List<LiveChannel> existing,
            Set<String> seenTvgIds) {

        Instant now = Instant.now();
        List<LiveChannel> toDelete = new ArrayList<>();

        for (LiveChannel channel : existing) {
            String tvgId = channel.getTvgId();

            if (tvgId != null
                    && !tvgId.isBlank()
                    && !seenTvgIds.contains(tvgId)) {

                channel.setDeleted(true);
                channel.setDeletedAt(now);
                channel.setStatus(LiveChannel.StreamStatus.OFFLINE);
                toDelete.add(channel);
            }
        }

        if (!toDelete.isEmpty()) {
            channelRepository.saveAll(toDelete);
        }

        return toDelete.size();
    }

    private void loadReferenceData()
            throws IOException, InterruptedException {

        JsonNode countries = readArray(countriesUrl);
        countryCodeToName.clear();

        for (JsonNode node : countries) {
            String code = text(node, "code");
            String name = text(node, "name");

            if (code != null && name != null) {
                countryCodeToName.put(
                        code.trim().toUpperCase(Locale.ROOT),
                        name.trim()
                );
            }
        }

        JsonNode categories = readArray(categoriesUrl);
        categoryIdToName.clear();

        for (JsonNode node : categories) {
            String id = text(node, "id");
            String name = text(node, "name");

            if (id != null && name != null) {
                categoryIdToName.put(id.trim(), name.trim());
            }
        }

        JsonNode languages = readArray(languagesUrl);
        languageCodeToName.clear();

        for (JsonNode node : languages) {
            String code = text(node, "code");
            String name = text(node, "name");

            if (code != null && name != null) {
                languageCodeToName.put(code.trim(), name.trim());
            }
        }

        log.info(
                "Loaded IPTV-org reference data: countries={}, categories={}, languages={}",
                countryCodeToName.size(),
                categoryIdToName.size(),
                languageCodeToName.size()
        );
    }

    private void buildFeedLanguageMap(JsonNode feeds) {
        channelToLanguage.clear();

        for (JsonNode feed : feeds) {
            String channel = text(feed, "channel");
            if (channel == null || channel.isBlank()) {
                continue;
            }

            JsonNode languages = feed.path("languages");
            if (!languages.isArray() || languages.isEmpty()) {
                continue;
            }

            String code = languages.get(0).asText(null);
            if (code == null || code.isBlank()) {
                continue;
            }

            code = code.trim();

            String display = languageCodeToName.getOrDefault(code, code);
            boolean isMain = feed.path("is_main").asBoolean(false);

            if (!channelToLanguage.containsKey(channel) || isMain) {
                channelToLanguage.put(channel, display);
            }
        }
    }

    private void buildLogoMap(JsonNode logos) {
        channelToLogo.clear();

        for (JsonNode logo : logos) {
            String channel = text(logo, "channel");
            String url = normalizeUrl(text(logo, "url"));

            if (channel == null || channel.isBlank() || url == null) {
                continue;
            }

            boolean inUse = logo.path("in_use").asBoolean(false);

            if (inUse || !channelToLogo.containsKey(channel)) {
                channelToLogo.put(channel, url);
            }
        }
    }

    private String resolvePrimaryCategory(JsonNode categories) {
        if (!categories.isArray() || categories.isEmpty()) {
            return "General";
        }

        for (JsonNode category : categories) {
            String id = category.asText(null);
            if (id != null && !"general".equalsIgnoreCase(id)) {
                return categoryIdToName.getOrDefault(id, prettify(id));
            }
        }

        String id = categories.get(0).asText(null);
        return id == null
                ? "General"
                : categoryIdToName.getOrDefault(id, prettify(id));
    }

    /**
     * Saves a LiveChannel with retry logic for optimistic locking exceptions.
     * Refreshes the entity state and retries up to 3 times with exponential backoff.
     *
     * @param channel the channel to save
     * @return the saved channel
     */
    private LiveChannel saveWithOptimisticLockingRetry(LiveChannel channel) {
        int maxAttempts = 3;
        int baseDelayMs = 10;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return channelRepository.save(channel);
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == maxAttempts) {
                    log.warn("Failed to save channel after {} attempts due to optimistic locking: {}",
                            maxAttempts, channel.getId(), e);
                    throw e; // Re-throw after final attempt
                }

                log.debug("Optimistic locking exception on attempt {}, refreshing entity and retrying: {}",
                        attempt, channel.getId());

                // Refresh the entity to get latest state from database
                if (channel.getId() != null) {
                    Optional<LiveChannel> refreshed = channelRepository.findByIdAndDeletedFalse(channel.getId());
                    if (refreshed.isPresent()) {
                        // Copy the changes we want to persist to the refreshed entity
                        LiveChannel refreshedChannel = refreshed.get();
                        refreshedChannel.setName(channel.getName());
                        refreshedChannel.setCountryCode(channel.getCountryCode());
                        refreshedChannel.setCountry(channel.getCountry());
                        refreshedChannel.setCategory(channel.getCategory());
                        refreshedChannel.setLanguage(channel.getLanguage());
                        refreshedChannel.setLogo(channel.getLogo());
                        refreshedChannel.setTvgName(channel.getTvgName());
                        refreshedChannel.setTvgLogo(channel.getTvgLogo());
                        refreshedChannel.setGroupTitle(channel.getGroupTitle());
                        refreshedChannel.setUrl(channel.getUrl());
                        refreshedChannel.setHlsUrl(channel.getHlsUrl());
                        refreshedChannel.setNsfw(channel.isNsfw());
                        refreshedChannel.setDeleted(channel.isDeleted());
                        refreshedChannel.setDeletedAt(channel.getDeletedAt());
                        refreshedChannel.setDeletedBy(channel.getDeletedBy());
                        refreshedChannel.setStatus(channel.getStatus());
                        refreshedChannel.setLastChecked(channel.getLastChecked());
                        refreshedChannel.setViewCount(channel.getViewCount());
                        // Note: favorite status is toggled via toggleFavorite(), not here
                        channel = refreshedChannel; // Use refreshed entity for next attempt
                    }
                }

                // Exponential backoff: 10ms, 20ms, 40ms
                try {
                    Thread.sleep(baseDelayMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Should not reach here due to throw in loop, but just in case
        throw new IllegalStateException("Unexpected state in saveWithOptimisticLockingRetry");
    }

    private JsonNode chooseBestStream(List<JsonNode> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        return candidates.stream()
                .filter(node -> normalizeUrl(text(node, "url")) != null)
                .sorted((a, b) -> {
                    int penalty = Integer.compare(
                            streamPenalty(a),
                            streamPenalty(b)
                    );

                    if (penalty != 0) {
                        return penalty;
                    }

                    return Integer.compare(
                            qualityRank(b),
                            qualityRank(a)
                    );
                })
                .findFirst()
                .orElse(null);
    }

    private int streamPenalty(JsonNode node) {
        JsonNode labels = node.path("labels");
        int penalty = 0;

        if (labels.isArray()) {
            for (JsonNode label : labels) {
                String value = label.asText();

                if ("Geo-blocked".equalsIgnoreCase(value)) {
                    penalty += 100;
                } else if ("Not 24/7".equalsIgnoreCase(value)) {
                    penalty += 10;
                }
            }
        }

        return penalty;
    }

    private int qualityRank(JsonNode node) {
        String quality = text(node, "quality");
        return numericQuality(quality);
    }

    private Map<String, List<JsonNode>> groupBy(
            JsonNode array,
            String field) {

        Map<String, List<JsonNode>> result = new HashMap<>();

        if (array == null || !array.isArray()) {
            return result;
        }

        for (JsonNode node : array) {
            String key = text(node, field);

            if (key != null && !key.isBlank()) {
                result.computeIfAbsent(
                        key.trim(),
                        ignored -> new ArrayList<>()
                ).add(node);
            }
        }

        return result;
    }

    private record M3uEntry(
            String channelId,
            String feedId,
            String url,
            String quality,
            List<String> labels) {
    }

    private Map<String, List<M3uEntry>> parseM3u(String m3uContent) {
        Map<String, List<M3uEntry>> result = new HashMap<>();

        if (m3uContent == null || m3uContent.isBlank()) {
            return result;
        }

        String[] lines = m3uContent.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (!line.startsWith("#EXTINF:")) {
                continue;
            }

            String tvgId = extractAttribute(line, "tvg-id");
            if (tvgId == null || tvgId.isBlank()) {
                continue;
            }

            String url = null;

            for (int j = i + 1; j < lines.length; j++) {
                String nextLine = lines[j].trim();

                if (nextLine.isEmpty()) {
                    continue;
                }

                if (!nextLine.startsWith("#")) {
                    url = nextLine;
                    break;
                }

                if (nextLine.startsWith("#EXTINF:")) {
                    break;
                }
            }

            url = normalizeUrl(url);

            if (url == null) {
                continue;
            }

            String channelId;
            String feedId;

            int at = tvgId.indexOf('@');

            if (at > 0 && at < tvgId.length() - 1) {
                channelId = tvgId.substring(0, at);
                feedId = tvgId.substring(at + 1);
            } else {
                channelId = tvgId;
                feedId = "SD";
            }

            String quality = extractAttribute(line, "quality");
            if (quality == null || quality.isBlank()) {
                quality = "Unknown";
            }

            List<String> labels = new ArrayList<>();
            String groupTitle = extractAttribute(line, "group-title");

            if (groupTitle != null && !groupTitle.isBlank()) {
                labels.add(groupTitle);
            }

            M3uEntry entry = new M3uEntry(
                    channelId.trim(),
                    feedId.trim(),
                    url,
                    quality.trim(),
                    labels
            );

            result.computeIfAbsent(
                    channelId.trim(),
                    ignored -> new ArrayList<>()
            ).add(entry);
        }

        return result;
    }

    private String extractAttribute(String line, String attribute) {
        if (line == null || attribute == null || attribute.isBlank()) {
            return null;
        }

        String pattern = "(?i)(?:^|\\s)"
                + java.util.regex.Pattern.quote(attribute)
                + "=\"([^\"]*)\"";

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile(pattern)
                        .matcher(line);

        return matcher.find() ? matcher.group(1) : null;
    }

    private M3uEntry chooseBestM3uEntry(
            List<M3uEntry> candidates) {

        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        return candidates.stream()
                .filter(entry -> normalizeUrl(entry.url()) != null)
                .sorted((a, b) -> {
                    int penalty = Integer.compare(
                            m3uEntryPenalty(a),
                            m3uEntryPenalty(b)
                    );

                    if (penalty != 0) {
                        return penalty;
                    }

                    return Integer.compare(
                            m3uQualityRank(b),
                            m3uQualityRank(a)
                    );
                })
                .findFirst()
                .orElse(null);
    }

    private int m3uEntryPenalty(M3uEntry entry) {
        int penalty = 0;

        if (entry.labels() != null) {
            for (String label : entry.labels()) {
                if ("Geo-blocked".equalsIgnoreCase(label)) {
                    penalty += 100;
                } else if ("Not 24/7".equalsIgnoreCase(label)) {
                    penalty += 10;
                }
            }
        }

        return penalty;
    }

    private int m3uQualityRank(M3uEntry entry) {
        return numericQuality(entry.quality());
    }

    private int numericQuality(String quality) {
        if (quality == null || quality.isBlank()) {
            return 0;
        }

        try {
            String digits = quality.replaceAll("[^0-9]", "");
            return digits.isBlank()
                    ? 0
                    : Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private JsonNode readArray(String url)
            throws IOException, InterruptedException {

        String body = fetchUrl(url);
        JsonNode node = objectMapper.readTree(body);

        if (!node.isArray()) {
            throw new IOException("Expected JSON array from " + url);
        }

        return node;
    }

    private String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }

        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isTextual()
                || value.isNumber()
                || value.isBoolean()) {
            return value.asText();
        }

        return null;
    }

    private String prettify(String value) {
        if (value == null || value.isBlank()) {
            return "General";
        }

        String normalized = value.trim().replace('-', ' ');

        if (normalized.isEmpty()) {
            return "General";
        }

        return Character.toUpperCase(normalized.charAt(0))
                + normalized.substring(1);
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }

        String normalized = url.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (!normalized.startsWith("http://")
                && !normalized.startsWith("https://")) {
            return null;
        }

        return normalized;
    }

    private boolean isHls(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String lower = url.toLowerCase(Locale.ROOT);

        return lower.contains(".m3u8")
                || lower.contains("application/vnd.apple.mpegurl");
    }

    @Scheduled(
            fixedRateString = "${livetv.health-check.interval-ms:1800000}"
    )
    @Async("healthCheckExecutor")
    public void healthCheckChannels() {
        if (!healthCheckEnabled) {
            log.debug("Live TV health check is disabled.");
            return;
        }

        int batchSize = Math.max(
                1,
                Math.min(maxConcurrentChecks, DEFAULT_HEALTH_BATCH_SIZE)
        );

        try {
            List<LiveChannel> channels =
                    channelRepository.findAllByStatusInAndDeletedFalse(
                            List.of(
                                    LiveChannel.StreamStatus.UNKNOWN,
                                    LiveChannel.StreamStatus.CHECKING,
                                    LiveChannel.StreamStatus.OFFLINE
                            ),
                            PageRequest.of(0, batchSize)
                    );

            int onlineBatchSize = Math.max(1, batchSize / 2);

            List<LiveChannel> online =
                    channelRepository.findTopOnlineChannels(
                            PageRequest.of(0, onlineBatchSize)
                    );

            Set<UUID> seen = new HashSet<>();
            List<LiveChannel> toCheck = new ArrayList<>(
                    channels.size() + online.size()
            );

            for (LiveChannel channel : channels) {
                if (channel.getId() != null && seen.add(channel.getId())) {
                    toCheck.add(channel);
                }
            }

            for (LiveChannel channel : online) {
                if (channel.getId() != null && seen.add(channel.getId())) {
                    toCheck.add(channel);
                }
            }

            int onlineCount = 0;
            int offlineCount = 0;

            for (LiveChannel channel : toCheck) {
                try {
                    channel.markChecking();
                    saveWithOptimisticLockingRetry(channel);

                    long started = System.nanoTime();
                    boolean healthy = checkStreamHealth(channel.getUrl());

                    int responseMs = elapsedMillis(started);

                    if (healthy) {
                        channel.markOnline(responseMs);
                        onlineCount++;
                    } else {
                        channel.markOffline(
                                "Stream is not accessible from the server"
                        );
                        offlineCount++;
                    }

                    saveWithOptimisticLockingRetry(channel);

                } catch (Exception e) {
                    channel.markOffline(
                            safeErrorMessage(e, "Health check failed")
                    );
                    saveWithOptimisticLockingRetry(channel);
                    offlineCount++;
                }
            }

            clearCaches();

            log.info(
                    "Live TV health check completed: checked={}, online={}, offline={}",
                    toCheck.size(),
                    onlineCount,
                    offlineCount
            );

        } catch (Exception e) {
            log.error("Live TV health check failed", e);
        }
    }

    private boolean checkStreamHealth(String url) {
        String normalizedUrl = normalizeUrl(url);

        if (normalizedUrl == null) {
            return false;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(
                            Math.max(1, healthCheckTimeoutSeconds)
                    ))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(
                            Math.max(1, healthCheckTimeoutSeconds)
                    ))
                    .header(
                            "User-Agent",
                            "Mozilla/5.0 (compatible; VideoPlatform-LiveTV/1.0)"
                    )
                    .header("Range", "bytes=0-1024")
                    .build();

            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            int statusCode = response.statusCode();

            return statusCode >= 200 && statusCode < 400;

        } catch (Exception e) {
            log.debug(
                    "Stream health check failed for {}: {}",
                    normalizedUrl,
                    e.getMessage()
            );
            return false;
        }
    }

    @Cacheable(
            value = "liveChannels",
            key = "#country + ':' + #category + ':' + #search + ':' + #page + ':' + #size + ':' + #sort"
    )
    @Transactional(readOnly = true)
    public Page<LiveChannel> getChannels(
            String country,
            String category,
            String search,
            int page,
            int size,
            String sort) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(
                Math.max(1, size),
                MAX_PAGE_SIZE
        );

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                parseSort(sort)
        );

        String normalizedCountry = normalizeFilter(country, true);
        String normalizedCategory = normalizeFilter(category, false);
        String normalizedSearch = normalizeFilter(search, false);

        return channelRepository.findWithFilters(
                normalizedCountry,
                normalizedCategory,
                normalizedSearch,
                pageable
        );
    }

    @Cacheable(
            value = "liveCountries",
            key = "(#category == null ? '' : #category) + ':' + (#search == null ? '' : #search)"
    )
    @Transactional(readOnly = true)
    public List<CountryStats> getCountries(String category, String search) {
        return channelRepository.getCountryStats(
                normalizeFilter(category, false),
                normalizeFilter(search, false)
        );
    }

    @Cacheable(
            value = "liveCategories",
            key = "(#country == null ? '' : #country) + ':' + (#search == null ? '' : #search)"
    )
    @Transactional(readOnly = true)
    public List<CategoryStats> getCategories(String country, String search) {
        return channelRepository.getCategoryStats(
                normalizeFilter(country, true),
                normalizeFilter(search, false)
        );
    }

    @Cacheable(value = "liveChannel", key = "#id")
    @Transactional(readOnly = true)
    public Optional<LiveChannel> getChannel(UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        return channelRepository.findByIdAndDeletedFalse(id);
    }

    @Transactional(readOnly = true)
    public Optional<LiveChannel> getChannelByTvgId(String tvgId) {
        if (tvgId == null || tvgId.isBlank()) {
            return Optional.empty();
        }

        return channelRepository.findByTvgIdAndDeletedFalse(tvgId.trim());
    }

    @Transactional
    public LiveChannel toggleFavorite(UUID channelId) {
        LiveChannel channel = channelRepository
                .findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Channel not found: " + channelId
                ));

        channel.toggleFavorite();

        LiveChannel saved = saveWithOptimisticLockingRetry(channel);
        clearCaches();

        return saved;
    }

    @Transactional
    public LiveChannel checkChannel(UUID channelId) {
        LiveChannel channel = channelRepository
                .findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Channel not found: " + channelId
                ));

        channel.markChecking();
        saveWithOptimisticLockingRetry(channel);

        long started = System.nanoTime();
        boolean healthy = checkStreamHealth(channel.getUrl());
        int responseMs = elapsedMillis(started);

        if (healthy) {
            channel.markOnline(responseMs);
        } else {
            channel.markOffline(
                    "Stream is not accessible from the server"
            );
        }

        LiveChannel saved = saveWithOptimisticLockingRetry(channel);
        clearCaches();

        return saved;
    }

    @Transactional(readOnly = true)
    public List<LiveChannel> getFeatured() {
        return channelRepository.findTopOnlineChannels(
                PageRequest.of(0, DEFAULT_FEATURED_SIZE)
        );
    }

    @Transactional
    public LiveChannel recordView(UUID channelId) {
        LiveChannel channel = channelRepository
                .findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Channel not found: " + channelId
                ));

        channel.incrementViewCount();

        LiveChannel saved = saveWithOptimisticLockingRetry(channel);
        clearCaches();

        return saved;
    }

    private Sort parseSort(String sort) {
        String value = sort == null || sort.isBlank()
                ? "name,asc"
                : sort.trim();

        String[] parts = value.split(",", -1);

        String property = parts[0].trim();

        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            property = "name";
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length > 1
                && "desc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.DESC;
        }

        return Sort.by(direction, property);
    }

    private String normalizeFilter(
            String value,
            boolean upperCase) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        return upperCase
                ? normalized.toUpperCase(Locale.ROOT)
                : normalized;
    }

    private int elapsedMillis(long startedNanos) {
        return (int) Math.min(
                Integer.MAX_VALUE,
                Duration.ofNanos(
                        System.nanoTime() - startedNanos
                ).toMillis()
        );
    }

    private String safeErrorMessage(
            Exception e,
            String fallback) {

        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            return fallback;
        }

        return message.length() > 500
                ? message.substring(0, 500)
                : message;
    }

    private String fetchUrl(String url)
            throws IOException, InterruptedException {

        if (url == null || url.isBlank()) {
            throw new IOException("URL is blank");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofMinutes(2))
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "VideoPlatform-LiveTV/1.0")
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IOException(
                    "Failed to fetch "
                            + url
                            + ": HTTP "
                            + response.statusCode()
            );
        }

        return response.body();
    }

    private void clearCaches() {
        clearCache("liveChannels");
        clearCache("liveChannel");
        clearCache("liveCountries");
        clearCache("liveCategories");
    }

    private void clearCache(String name) {
        var cache = cacheManager.getCache(name);

        if (cache != null) {
            cache.clear();
        }
    }
}

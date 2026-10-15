package com.platform.livetv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.platform.livetv.dto.CategoryStats;
import com.platform.livetv.dto.CountryStats;
import com.platform.livetv.entity.LiveChannel;
import com.platform.livetv.entity.LiveStreamCandidate;
import com.platform.livetv.repository.LiveStreamCandidateRepository;
import com.platform.livetv.repository.LiveChannelRepository;
import com.platform.livetv.repository.UserFavoriteRepository;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.livetv.search.LiveChannelSearchIndex;
import jakarta.annotation.PostConstruct;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.net.URLEncoder;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.data.domain.PageImpl;

@Service
public class IptvOrgSyncService {

    private static final Logger log = LoggerFactory.getLogger(IptvOrgSyncService.class);

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_HEALTH_BATCH_SIZE = 50;
    private static final int DEFAULT_FEATURED_SIZE = 20;
    private static final int SYNC_BATCH_SIZE = 100;
    private static final int SYNC_BATCH_RETRIES = 4;

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
    private final LiveStreamCandidateRepository streamCandidateRepository;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final CacheManager cacheManager;
    private final LiveChannelSearchIndex searchIndex = new LiveChannelSearchIndex();
    private final UserFavoriteRepository userFavoriteRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final TransactionTemplate syncTransactionTemplate;
    private final HttpClient iptvCompatibilityClient = buildIptvCompatibilityClient();

    /** In-memory local IPTV catalogue used by the proxy for header-aware failover. */
    private volatile Map<String, List<M3uEntry>> localM3uByChannel = Map.of();
    private volatile Map<String, List<JsonNode>> remoteStreamsByChannel = Map.of();
    private volatile Map<String, StreamCandidate> streamHeadersByUrl = Map.of();
    /**
     * The manifest response establishes the upstream feed used for this playback
     * session. Child playlists, segments and keys often use different URLs, so
     * exact URL lookup alone would lose the feed's required Referer/User-Agent.
     */
    private final Map<UUID, StreamCandidate> selectedProxyCandidates = new ConcurrentHashMap<>();
    /** Last observed playback state per channel. Kept in-memory so playback diagnostics never depend on DB writes. */
    private final Map<UUID, PlaybackSnapshot> playbackSnapshots = new ConcurrentHashMap<>();
    /** Cached browser-CORS compatibility so the web client only attempts direct HLS when the provider explicitly permits it. */
    private final Map<String, BrowserCompatibility> browserCompatibilityByUrl = new ConcurrentHashMap<>();
    private static final int MAX_PLAYBACK_ATTEMPTS_PER_DIAGNOSTIC = 32;
    private static final long MAX_PROXY_RESPONSE_BYTES = 32L * 1024L * 1024L;
    private static final long BROWSER_COMPATIBILITY_CACHE_MS = 10 * 60 * 1000L;

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

    /**
     * Optional local IPTV-org checkout. When present, streams are read directly
     * from the repository instead of downloading streams.json. The importer
     * keeps the remote API as a fallback so existing deployments continue to work.
     */
    @Value("${livetv.iptv-org.local-repo-path:}")
    private String localRepoPath;

    @Value("${livetv.iptv-org.local-streams-path:}")
    private String localStreamsPath;

    @Value("${livetv.playback.browser-origin:http://localhost:3000}")
    private String browserOrigin;

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
    private final Map<String, Path> localLogoFiles = new ConcurrentHashMap<>();
    /** Prevents startup/scheduled synchronization from racing with health checks. */
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    public IptvOrgSyncService(
            LiveChannelRepository channelRepository,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper,
            @Qualifier("xmlMapper") XmlMapper xmlMapper,
            CacheManager cacheManager,
            LiveStreamCandidateRepository streamCandidateRepository,
            UserFavoriteRepository userFavoriteRepository,
            CurrentUserAccessor currentUserAccessor,
            PlatformTransactionManager transactionManager) {
        this.channelRepository = channelRepository;
        this.streamCandidateRepository = streamCandidateRepository;
        this.objectMapper = objectMapper;
        this.xmlMapper = xmlMapper;
        this.cacheManager = cacheManager;
        this.userFavoriteRepository = userFavoriteRepository;
        this.currentUserAccessor = currentUserAccessor;
        this.syncTransactionTemplate = new TransactionTemplate(transactionManager);
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

    /** True while the startup/scheduled IPTV catalogue synchronization is running. */
    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }

    /** Number of currently visible catalogue channels. */
    @Transactional(readOnly = true)
    public long getCatalogChannelCount() {
        return channelRepository.countByDeletedFalse();
    }

    /**
     * The UI uses this instead of treating an empty catalogue during startup as
     * a real "0 channels" result. This prevents React Query from caching the
     * startup-empty response for minutes while the background sync is still
     * importing the local IPTV-org checkout.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getReadiness() {
        long count = channelRepository.countByDeletedFalse();
        boolean syncing = syncInProgress.get();
        return Map.of(
                "ready", !syncing && count > 0,
                "syncInProgress", syncing,
                "channelCount", count
        );
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

        if (!syncInProgress.compareAndSet(false, true)) {
            log.debug("IPTV-org sync skipped because another synchronization is already running.");
            return;
        }

        long started = System.nanoTime();
        log.info("Starting IPTV-org Live TV sync...");

        try {
            loadReferenceData();

            JsonNode channels = readLocalOrRemoteArray("data/channels.json", channelsUrl);
            JsonNode feeds = readLocalOrRemoteArray("data/feeds.json", feedsUrl);
            JsonNode logos = readLocalOrRemoteArray("data/logos.json", logosUrl);

            Map<String, List<JsonNode>> streamsByChannel = new HashMap<>();
            Map<String, List<M3uEntry>> m3uByChannel = loadLocalM3uStreams();
            localM3uByChannel = m3uByChannel;
            if (!m3uByChannel.isEmpty()) {
                log.info(
                        "Using local IPTV-org streams checkout: channels={}, path={}",
                        m3uByChannel.size(),
                        resolveLocalStreamsRoot()
                );
            } else {
                try {
                    streamsByChannel = groupBy(readArray(streamsUrl), "channel");
                } catch (Exception e) {
                    log.warn(
                            "Unable to load IPTV-org streams.json; falling back to M3U: {}",
                            e.getMessage()
                    );
                }

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
            }

            remoteStreamsByChannel = streamsByChannel;
            Map<String, StreamCandidate> discoveredHeaders = new HashMap<>();
            streamsByChannel.values().stream().flatMap(List::stream).forEach(node -> {
                String url = normalizeUrl(text(node, "url"));
                if (url != null) {
                    discoveredHeaders.put(url, new StreamCandidate(
                            url,
                            normalizeHeader(firstText(node, "referrer", "http_referrer")),
                            normalizeHeader(text(node, "user_agent"))
                    ));
                }
            });
            streamHeadersByUrl = discoveredHeaders;
            localM3uByChannel = m3uByChannel;

            buildFeedLanguageMap(feeds);
            buildLogoMap(logos);

            List<ChannelSyncData> syncRows = new ArrayList<>();
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

                    String normalizedTvgId = tvgId.trim();
                    List<JsonNode> remoteCandidates = streamsByChannel.getOrDefault(normalizedTvgId, List.of());
                    List<M3uEntry> localCandidates = m3uByChannel.getOrDefault(baseChannelId(normalizedTvgId), List.of());

                    String streamUrl = null;
                    JsonNode bestStream = chooseBestStream(remoteCandidates);
                    if (bestStream != null) {
                        streamUrl = text(bestStream, "url");
                    }
                    if (streamUrl == null || streamUrl.isBlank()) {
                        M3uEntry bestEntry = chooseBestM3uEntry(localCandidates);
                        if (bestEntry != null) streamUrl = bestEntry.url();
                    }
                    streamUrl = normalizeUrl(streamUrl);

                    int candidateCount = countUniqueCandidates(remoteCandidates, localCandidates);
                    if (candidateCount == 0 && streamUrl == null) {
                        skippedNoStream++;
                    }

                    Set<String> categories = resolveCategories(channelNode.path("categories"));
                    String category = categories.stream().findFirst().orElse("General");
                    String language = channelToLanguage.getOrDefault(normalizedTvgId, "Unknown");
                    String logoUrl = normalizeUrl(channelToLogo.get(normalizedTvgId));

                    syncRows.add(new ChannelSyncData(
                            normalizedTvgId,
                            name.trim(),
                            countryCode,
                            countryCodeToName.getOrDefault(countryCode, countryCode),
                            categories,
                            category,
                            language,
                            logoUrl,
                            streamUrl,
                            channelNode.path("is_nsfw").asBoolean(false),
                            remoteCandidates,
                            localCandidates,
                            candidateCount
                    ));
                    seenTvgIds.add(normalizedTvgId);
                } catch (Exception itemError) {
                    skippedInvalid++;
                    log.debug("Skipping malformed IPTV-org channel", itemError);
                }
            }

            SyncPersistenceResult persistence = persistSyncRows(syncRows);
            int softDeleted = softDeleteMissingChannels(List.of(), seenTvgIds);

            // The index is rebuilt only after every successful batch has committed.
            // A partial database write must never masquerade as a complete catalogue.
            rebuildSearchIndex();
            clearCaches();

            long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            log.info(
                    "IPTV-org sync completed: parsed={}, persisted={}, failedBatches={}, candidates={}, softDeleted={}, skippedNoStream={}, skippedInvalid={}, elapsedMs={}",
                    syncRows.size(),
                    persistence.persistedChannels(),
                    persistence.failedBatches(),
                    persistence.candidateCount(),
                    softDeleted,
                    skippedNoStream,
                    skippedInvalid,
                    elapsedMs
            );

            if (persistence.failedBatches() > 0) {
                log.warn("IPTV-org sync completed with {} failed batch(es); successful batches remain committed and will be retried on the next scheduled sync.", persistence.failedBatches());
            }

        } catch (Exception e) {
            log.error("IPTV-org sync failed", e);
        } finally {
            syncInProgress.set(false);
        }
    }

    private int softDeleteMissingChannels(List<LiveChannel> existing, Set<String> seenTvgIds) {
        // IPTV-org metadata and streams can be temporarily incomplete. V10 never
        // deletes a channel merely because one sync could not see its stream.
        return 0;
    }

    private void loadReferenceData()
            throws IOException, InterruptedException {

        JsonNode countries = readLocalOrRemoteArray("data/countries.json", countriesUrl);
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

        JsonNode categories = readLocalOrRemoteArray("data/categories.json", categoriesUrl);
        categoryIdToName.clear();

        for (JsonNode node : categories) {
            String id = text(node, "id");
            String name = text(node, "name");

            if (id != null && name != null) {
                categoryIdToName.put(id.trim(), name.trim());
            }
        }

        JsonNode languages = readLocalOrRemoteArray("data/languages.json", languagesUrl);
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
        localLogoFiles.clear();
        indexLocalLogoFiles();

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

    private void indexLocalLogoFiles() {
        Path root = resolveLocalRepoRoot();
        if (root == null) return;
        Path logos = root.resolve("logos");
        if (!Files.isDirectory(logos)) return;
        try (var paths = Files.walk(logos)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String name = path.getFileName().toString();
                int dot = name.lastIndexOf('.');
                if (dot <= 0) return;
                String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (!Set.of("png", "jpg", "jpeg", "webp", "svg", "gif", "avif").contains(ext)) return;
                String channelId = name.substring(0, dot).trim().toLowerCase(Locale.ROOT);
                if (!channelId.isBlank()) localLogoFiles.putIfAbsent(channelId, path.toAbsolutePath().normalize());
            });
            log.info("Indexed local IPTV-org logos: files={}, path={}", localLogoFiles.size(), logos.toAbsolutePath().normalize());
        } catch (IOException e) {
            log.warn("Unable to scan local IPTV-org logos {}: {}", logos, e.getMessage());
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
                        refreshedChannel.setCategories(channel.getCategories());
                        refreshedChannel.setLanguage(channel.getLanguage());
                        refreshedChannel.setLogo(channel.getLogo());
                        refreshedChannel.setTvgName(channel.getTvgName());
                        refreshedChannel.setTvgLogo(channel.getTvgLogo());
                        refreshedChannel.setGroupTitle(channel.getGroupTitle());
                        refreshedChannel.setUrl(channel.getUrl());
                        refreshedChannel.setHlsUrl(channel.getHlsUrl());
                        refreshedChannel.setStreamCandidateCount(channel.getStreamCandidateCount());
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

    private Set<String> resolveCategories(JsonNode categories) {
        Set<String> result = new java.util.LinkedHashSet<>();
        Set<String> general = new java.util.LinkedHashSet<>();
        if (categories != null && categories.isArray()) {
            for (JsonNode category : categories) {
                String id = category.asText(null);
                if (id == null || id.isBlank()) continue;
                String name = categoryIdToName.getOrDefault(id.trim(), prettify(id));
                if ("General".equalsIgnoreCase(name)) general.add(name); else result.add(name);
            }
        }
        if (result.isEmpty()) result.addAll(general);
        if (result.isEmpty()) result.add("General");
        return result;
    }

    private void replacePersistentCandidates(LiveChannel channel, List<JsonNode> remote, List<M3uEntry> local) {
        if (channel.getId() == null) return;
        if (remote.isEmpty() && local.isEmpty()) {
            // A transiently incomplete IPTV-org checkout must never erase the last
            // known-good candidate set. The next sync/health cycle can retire it
            // after repeated failures, but playback keeps a chance to recover now.
            channel.setStreamCandidateCount((int) Math.min(Integer.MAX_VALUE, streamCandidateRepository.countByChannelId(channel.getId())));
            if (channel.getStreamCandidateCount() > 0 && channel.getStatus() == LiveChannel.StreamStatus.NO_SOURCE) {
                channel.setStatus(LiveChannel.StreamStatus.DEGRADED);
            }
            return;
        }
        streamCandidateRepository.deleteAllByChannelId(channel.getId());
        streamCandidateRepository.flush();
        Map<String, LiveStreamCandidate> unique = new java.util.LinkedHashMap<>();
        for (M3uEntry entry : local) {
            String url = normalizeUrl(entry.url());
            if (url == null) continue;
            LiveStreamCandidate c = new LiveStreamCandidate();
            c.setChannelId(channel.getId());
            c.setFeedId(entry.feedId());
            c.setUrl(url);
            c.setQuality(entry.quality());
            c.setLabels(entry.labels() == null ? null : String.join(",", entry.labels()));
            c.setReferrer(entry.referrer());
            c.setUserAgent(entry.userAgent());
            unique.putIfAbsent(url.toLowerCase(Locale.ROOT), c);
        }
        for (JsonNode node : remote) {
            String url = normalizeUrl(text(node, "url"));
            if (url == null) continue;
            LiveStreamCandidate c = new LiveStreamCandidate();
            c.setChannelId(channel.getId());
            c.setFeedId(firstText(node, "feed", "id"));
            c.setUrl(url);
            c.setQuality(text(node, "quality"));
            JsonNode labels = node.path("labels");
            if (labels.isArray()) {
                List<String> ls = new ArrayList<>();
                labels.forEach(x -> ls.add(x.asText()));
                c.setLabels(String.join(",", ls));
            }
            c.setReferrer(firstText(node, "referrer", "http_referrer"));
            c.setUserAgent(text(node, "user_agent"));
            unique.putIfAbsent(url.toLowerCase(Locale.ROOT), c);
        }
        if (!unique.isEmpty()) streamCandidateRepository.saveAll(unique.values());
        channel.setStreamCandidateCount(unique.size());
        if (unique.isEmpty()) {
            channel.setStatus(LiveChannel.StreamStatus.NO_SOURCE);
        } else if (channel.getStatus() == LiveChannel.StreamStatus.NO_SOURCE) {
            channel.setStatus(LiveChannel.StreamStatus.UNKNOWN);
            channel.setCheckError(null);
        }
    }

    private int countUniqueCandidates(List<JsonNode> remote, List<M3uEntry> local) {
        Set<String> urls = new HashSet<>();
        for (M3uEntry entry : local) {
            String url = normalizeUrl(entry.url());
            if (url != null) urls.add(url.toLowerCase(Locale.ROOT));
        }
        for (JsonNode node : remote) {
            String url = normalizeUrl(text(node, "url"));
            if (url != null) urls.add(url.toLowerCase(Locale.ROOT));
        }
        return urls.size();
    }

    private SyncPersistenceResult persistSyncRows(List<ChannelSyncData> rows) {
        int persisted = 0;
        int failedBatches = 0;
        long candidateCount = 0;

        for (int start = 0; start < rows.size(); start += SYNC_BATCH_SIZE) {
            int end = Math.min(start + SYNC_BATCH_SIZE, rows.size());
            List<ChannelSyncData> batch = rows.subList(start, end);
            boolean committed = false;
            boolean isolatedFallback = false;

            for (int attempt = 1; attempt <= SYNC_BATCH_RETRIES; attempt++) {
                try {
                    syncTransactionTemplate.executeWithoutResult(status -> {
                        for (ChannelSyncData data : batch) {
                            LiveChannel channel = channelRepository.findByTvgId(data.tvgId()).orElseGet(LiveChannel::new);
                            boolean isNew = channel.getId() == null;

                            channel.setTvgId(data.tvgId());
                            channel.setName(data.name());
                            channel.setCountryCode(data.countryCode());
                            channel.setCountry(data.country());
                            channel.setCategories(data.categories());
                            channel.setCategory(data.category());
                            channel.setLanguage(data.language());
                            channel.setLogo(data.logoUrl());
                            channel.setTvgName(data.name());
                            channel.setTvgLogo(data.logoUrl());
                            channel.setGroupTitle(data.category());
                            channel.setUrl(data.streamUrl());
                            channel.setHlsUrl(data.streamUrl() != null && isHls(data.streamUrl()) ? data.streamUrl() : null);
                            channel.setNsfw(data.nsfw());
                            channel.setDeleted(false);
                            channel.setDeletedAt(null);
                            channel.setDeletedBy(null);
                            if (channel.getStatus() == null) channel.setStatus(LiveChannel.StreamStatus.UNKNOWN);

                            if (isNew) {
                                channelRepository.save(channel);
                                channelRepository.flush();
                            }

                            replacePersistentCandidates(channel, data.remoteCandidates(), data.localCandidates());
                        }
                        channelRepository.flush();
                    });
                    committed = true;
                    break;
                } catch (ObjectOptimisticLockingFailureException e) {
                    if (attempt == SYNC_BATCH_RETRIES) {
                        log.error("IPTV-org sync batch {}-{} failed after {} optimistic-lock retries", start, end, attempt, e);
                    } else {
                        log.warn("IPTV-org sync batch {}-{} hit optimistic locking on attempt {}/{}; retrying", start, end, attempt, SYNC_BATCH_RETRIES);
                        sleepQuietly(75L * attempt);
                    }
                } catch (RuntimeException e) {
                    log.error("IPTV-org sync batch {}-{} failed; isolating individual channels so one bad record cannot abort the catalogue", start, end, e);
                    int recovered = 0;
                    for (ChannelSyncData row : batch) {
                        if (persistSingleSyncRow(row)) {
                            recovered++;
                            candidateCount += row.candidateCount();
                        }
                    }
                    persisted += recovered;
                    if (recovered == batch.size()) committed = true;
                    else failedBatches++;
                    isolatedFallback = true;
                    break;
                }
            }

            if (isolatedFallback) {
                continue;
            }

            if (committed) {
                persisted += batch.size();
                candidateCount += batch.stream().mapToLong(ChannelSyncData::candidateCount).sum();
            } else {
                failedBatches++;
            }
        }

        return new SyncPersistenceResult(persisted, failedBatches, candidateCount);
    }

    private boolean persistSingleSyncRow(ChannelSyncData data) {
        for (int attempt = 1; attempt <= SYNC_BATCH_RETRIES; attempt++) {
            try {
                syncTransactionTemplate.executeWithoutResult(status -> {
                    LiveChannel channel = channelRepository.findByTvgId(data.tvgId()).orElseGet(LiveChannel::new);
                    boolean isNew = channel.getId() == null;
                    channel.setTvgId(data.tvgId());
                    channel.setName(data.name());
                    channel.setCountryCode(data.countryCode());
                    channel.setCountry(data.country());
                    channel.setCategories(data.categories());
                    channel.setCategory(data.category());
                    channel.setLanguage(data.language());
                    channel.setLogo(data.logoUrl());
                    channel.setTvgName(data.name());
                    channel.setTvgLogo(data.logoUrl());
                    channel.setGroupTitle(data.category());
                    channel.setUrl(data.streamUrl());
                    channel.setHlsUrl(data.streamUrl() != null && isHls(data.streamUrl()) ? data.streamUrl() : null);
                    channel.setNsfw(data.nsfw());
                    channel.setDeleted(false);
                    channel.setDeletedAt(null);
                    channel.setDeletedBy(null);
                    if (channel.getStatus() == null) channel.setStatus(LiveChannel.StreamStatus.UNKNOWN);
                    if (isNew) channelRepository.save(channel);
                    channelRepository.flush();
                    replacePersistentCandidates(channel, data.remoteCandidates(), data.localCandidates());
                    channelRepository.flush();
                });
                return true;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt < SYNC_BATCH_RETRIES) sleepQuietly(75L * attempt);
                else log.warn("Unable to persist IPTV channel {} after {} optimistic-lock retries", data.tvgId(), attempt, e);
            } catch (RuntimeException e) {
                log.warn("Unable to persist IPTV channel {}; skipping only this channel", data.tvgId(), e);
                return false;
            }
        }
        return false;
    }

    private record ChannelSyncData(
            String tvgId,
            String name,
            String countryCode,
            String country,
            Set<String> categories,
            String category,
            String language,
            String logoUrl,
            String streamUrl,
            boolean nsfw,
            List<JsonNode> remoteCandidates,
            List<M3uEntry> localCandidates,
            int candidateCount) {}

    private record SyncPersistenceResult(int persistedChannels, int failedBatches, long candidateCount) {}

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

        String url = text(node, "url");
        if (url == null || !isHls(url)) {
            penalty += 80;
        }

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

    /**
     * Reads all internal IPTV-org playlists from a local checkout. The repository
     * stores its source links as standard .m3u files under streams/. Reading the
     * checkout avoids a large streams.json download and also lets the UI catalogue
     * stay in sync with the exact Git clone on the host.
     */
    private Map<String, List<M3uEntry>> loadLocalM3uStreams() {
        Path root = resolveLocalStreamsRoot();
        if (root == null || !Files.isDirectory(root)) {
            return new HashMap<>();
        }

        Map<String, List<M3uEntry>> result = new HashMap<>();
        int filesRead = 0;

        try (var paths = Files.walk(root)) {
            List<Path> playlistFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".m3u"))
                    .toList();

            for (Path playlist : playlistFiles) {
                try {
                    String content = Files.readString(playlist, StandardCharsets.UTF_8);
                    mergeM3uEntries(result, parseM3u(content));
                    filesRead++;
                } catch (Exception e) {
                    log.debug("Skipping unreadable local IPTV playlist {}: {}", playlist, e.getMessage());
                }
            }

            log.info("Loaded local IPTV-org playlists: files={}, channels={}, path={}", filesRead, result.size(), root);
        } catch (IOException e) {
            log.warn("Unable to scan local IPTV-org streams directory {}: {}", root, e.getMessage());
        }

        return result;
    }

    private void mergeM3uEntries(
            Map<String, List<M3uEntry>> target,
            Map<String, List<M3uEntry>> source) {
        source.forEach((channelId, entries) ->
                target.computeIfAbsent(channelId, ignored -> new ArrayList<>()).addAll(entries));
    }

    private Path resolveLocalStreamsRoot() {
        List<Path> candidates = new ArrayList<>();

        if (localStreamsPath != null && !localStreamsPath.isBlank()) {
            candidates.add(Paths.get(localStreamsPath.trim()));
        }

        // Supports running from D:\\video-platform, backend\live-tv-service,
        // and IDE module working directories without requiring a machine-specific path.
        candidates.add(Paths.get("iptv", "streams"));
        candidates.add(Paths.get("..", "iptv", "streams"));
        candidates.add(Paths.get("..", "..", "iptv", "streams"));

        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(absolute)) {
                return absolute;
            }
        }

        return null;
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
            List<String> labels,
            String referrer,
            String userAgent) {
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
            String referrer = null;
            String userAgent = null;

            for (int j = i + 1; j < lines.length; j++) {
                String nextLine = lines[j].trim();

                if (nextLine.isEmpty()) {
                    continue;
                }

                if (!nextLine.startsWith("#")) {
                    url = nextLine;
                    break;
                }

                if (nextLine.startsWith("#EXTVLCOPT:http-referrer=")) {
                    referrer = nextLine.substring("#EXTVLCOPT:http-referrer=".length()).trim();
                    continue;
                }

                if (nextLine.startsWith("#EXTVLCOPT:http-user-agent=")) {
                    userAgent = nextLine.substring("#EXTVLCOPT:http-user-agent=".length()).trim();
                    continue;
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
                    labels,
                    normalizeHeader(referrer),
                    normalizeHeader(userAgent)
            );

            result.computeIfAbsent(
                    channelId.trim(),
                    ignored -> new ArrayList<>()
            ).add(entry);
        }

        return result;
    }

    private String baseChannelId(String tvgId) {
        if (tvgId == null || tvgId.isBlank()) {
            return "";
        }
        int at = tvgId.indexOf('@');
        return (at > 0 ? tvgId.substring(0, at) : tvgId).trim();
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
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

        if (entry.url() == null || !isHls(entry.url())) {
            penalty += 80;
        }

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

    private JsonNode readLocalOrRemoteArray(String relativePath, String remoteUrl)
            throws IOException, InterruptedException {

        Path root = resolveLocalRepoRoot();
        if (root != null) {
            Path localFile = root.resolve(relativePath).normalize();
            if (Files.isRegularFile(localFile)) {
                try {
                    String body = Files.readString(localFile, StandardCharsets.UTF_8);
                    JsonNode node = objectMapper.readTree(body);
                    if (node != null && node.isArray()) {
                        log.info("Loaded local IPTV-org data: {} (entries={})", localFile, node.size());
                        return node;
                    }
                } catch (Exception e) {
                    log.warn("Unable to read local IPTV-org data {}: {}", localFile, e.getMessage());
                }
            }
        }

        return readArray(remoteUrl);
    }

    private Path resolveLocalRepoRoot() {
        List<Path> candidates = new ArrayList<>();

        if (localRepoPath != null && !localRepoPath.isBlank()) {
            candidates.add(Paths.get(localRepoPath.trim()));
        }

        candidates.add(Paths.get("iptv"));
        candidates.add(Paths.get("..", "iptv"));
        candidates.add(Paths.get("..", "..", "iptv"));

        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(absolute)
                    && (Files.isDirectory(absolute.resolve("streams"))
                        || Files.isDirectory(absolute.resolve("data")))) {
                return absolute;
            }
        }

        return null;
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

        // Startup sync can take tens of seconds with the full 325-file local checkout.
        // Running health updates against the same rows during that transaction used to
        // race the sync and cause optimistic-lock failures. Health checks are sampling,
        // so it is safer to defer this run until synchronization has finished.
        if (syncInProgress.get()) {
            log.debug("Live TV health check skipped while IPTV-org synchronization is in progress.");
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
                                    LiveChannel.StreamStatus.NO_SOURCE,
                                    LiveChannel.StreamStatus.DEGRADED
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
                    List<StreamCandidate> candidates = streamCandidates(channel);
                    int working = 0;
                    int firstResponseMs = 0;
                    for (StreamCandidate candidate : candidates) {
                        long candidateStarted = System.nanoTime();
                        if (checkStreamHealth(candidate.url(), candidate.referrer(), candidate.userAgent())) {
                            working++;
                            if (firstResponseMs == 0) firstResponseMs = elapsedMillis(candidateStarted);
                            break;
                        }
                    }
                    int responseMs = elapsedMillis(started);
                    if (working > 0) {
                        channel.markOnline(firstResponseMs > 0 ? firstResponseMs : responseMs);
                        onlineCount++;
                    } else if (!candidates.isEmpty()) {
                        channel.setStatus(LiveChannel.StreamStatus.DEGRADED);
                        channel.setLastChecked(Instant.now());
                        channel.setCheckError("No candidate responded from this server during health sampling");
                        channel.setResponseTimeMs(responseMs);
                        offlineCount++;
                    } else {
                        channel.markOffline("No stream candidates are currently indexed");
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

    private boolean checkStreamHealth(String url, String referrer, String userAgent) {
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

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(
                            Math.max(1, healthCheckTimeoutSeconds)
                    ))
                    .header(
                            "User-Agent",
                            normalizeHeader(userAgent) != null
                                    ? normalizeHeader(userAgent)
                                    : "Mozilla/5.0 (compatible; VideoPlatform-LiveTV/1.0)"
                    )
                    .header("Range", "bytes=0-1024");
            if (normalizeHeader(referrer) != null) {
                requestBuilder.header("Referer", normalizeHeader(referrer));
            }
            HttpRequest request = requestBuilder.build();

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

    /**
     * Browser-safe HLS proxy. IPTV sources frequently do not expose CORS headers,
     * so the React application must not fetch the provider URL directly. This
     * method fetches the provider resource server-side and rewrites HLS playlists
     * so every subsequent playlist/segment/key request stays inside this service.
     */
    @Transactional(readOnly = true)
    public StreamProxyResponse proxyStream(UUID channelId, String requestedUrl) {
        if (channelId == null) {
            throw new IllegalArgumentException("Channel id is required");
        }

        LiveChannel channel = channelRepository.findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Live channel not found"));

        String sourceUrl = normalizeUrl(
                channel.getHlsUrl() != null && !channel.getHlsUrl().isBlank()
                        ? channel.getHlsUrl()
                        : channel.getUrl()
        );
        if (sourceUrl == null) {
            throw new IllegalArgumentException("Live channel has no stream URL");
        }

        String targetUrl = requestedUrl == null || requestedUrl.isBlank()
                ? sourceUrl
                : normalizeUrl(requestedUrl);
        if (targetUrl == null) {
            throw new IllegalArgumentException("Invalid stream URL");
        }

        validateProxyTarget(targetUrl);

        // A manifest failure can be recovered by trying another IPTV-org feed.
        // Once HLS has produced a child segment URL, the request is tied to that
        // selected feed and we keep its exact headers instead of silently
        // switching media origins mid-playback.
        List<StreamCandidate> candidates = streamCandidates(channel);
        List<StreamCandidate> attempts = new ArrayList<>();
        StreamCandidate matched = candidateForUrl(channel, targetUrl);
        if (requestedUrl != null && !requestedUrl.isBlank() && matched == null) {
            throw new IllegalArgumentException("Requested stream URL is not associated with the selected Live TV channel");
        }
        attempts.add(new StreamCandidate(targetUrl,
                matched != null ? matched.referrer() : null,
                matched != null ? matched.userAgent() : null));

        if (requestedUrl == null || requestedUrl.isBlank()) {
            // A new manifest request starts a new feed selection session.
            selectedProxyCandidates.remove(channelId);
            for (StreamCandidate candidate : candidates) {
                if (!candidate.url().equalsIgnoreCase(targetUrl)) {
                    attempts.add(candidate);
                }
            }
        }

        List<String> failures = new ArrayList<>();
        List<PlaybackAttempt> observed = new ArrayList<>();
        for (StreamCandidate attempt : attempts) {
            try {
                StreamProxyResponse response = fetchProxyResource(channelId, attempt, observed);
                if (response != null) {
                    if (response.playlist()) {
                        selectedProxyCandidates.put(channelId, attempt);
                        recordPlaybackSuccess(channel, attempt, response);
                    }
                    return response;
                }
            } catch (IllegalStateException e) {
                String failure = e.getMessage() == null ? "upstream request failed" : e.getMessage();
                failures.add(failure);
                log.warn("Live TV upstream attempt failed: channelId={}, tvgId={}, url={}, referrer={}, reason={}",
                        channelId, channel.getTvgId(), attempt.url(), attempt.referrer(), failure);
                // A child playlist/segment belongs to the selected feed. Never silently
                // switch origins mid-HLS session; return the actual error to hls.js.
                if (requestedUrl != null && !requestedUrl.isBlank()) {
                    break;
                }
            }
        }

        PlaybackClassification classification = classifyFailures(attempts, observed, failures);
        recordPlaybackFailure(channel, observed, classification);

        String reason = failures.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("upstream request failed");
        String userMessage = classification == PlaybackClassification.UNAVAILABLE
                ? "Channel currently unavailable for free Live Streaming"
                : "Live stream could not be started. Please try again.";
        log.error("Live TV stream unavailable after {} candidate(s): channelId={}, tvgId={}, classification={}, failures={}",
                attempts.size(), channelId, channel.getTvgId(), classification, failures);
        throw new IllegalStateException(userMessage + " | reason=" + reason);
    }

    private StreamProxyResponse fetchProxyResource(UUID channelId, StreamCandidate candidate, List<PlaybackAttempt> observed) {
        Exception last = null;
        // Multiple request profiles are intentional. Some IPTV endpoints return 5xx
        // to one client fingerprint while serving the same M3U8 to a normal browser.
        List<RequestProfile> profiles = requestProfiles(candidate);
        for (int profileIndex = 0; profileIndex < profiles.size(); profileIndex++) {
            RequestProfile profile = profiles.get(profileIndex);
            for (int attempt = 1; attempt <= 2; attempt++) {
                long started = System.nanoTime();
                try {
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(URI.create(candidate.url()))
                            .GET()
                            .timeout(Duration.ofSeconds(30))
                            .header("Accept", profile.accept())
                            .header("Accept-Language", profile.acceptLanguage())
                            .header("Cache-Control", "no-cache")
                            .header("Pragma", "no-cache")
                            .header("User-Agent", profile.userAgent());

                    if (candidate.referrer() != null) {
                        builder.header("Referer", candidate.referrer());
                        try {
                            URI ref = URI.create(candidate.referrer());
                            if (ref.getScheme() != null && ref.getHost() != null) {
                                builder.header("Origin", ref.getScheme() + "://" + ref.getAuthority());
                            }
                        } catch (Exception ignored) { }
                    }

                    HttpResponse<byte[]> response = iptvCompatibilityClient.send(
                            builder.build(),
                            HttpResponse.BodyHandlers.ofByteArray()
                    );
                    int status = response.statusCode();
                    int elapsed = elapsedMillis(started);
                    String contentType = response.headers().firstValue("Content-Type").orElse("");
                    long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                    if (contentLength > MAX_PROXY_RESPONSE_BYTES) {
                        throw new IllegalStateException("Upstream response exceeds the Live TV proxy safety limit");
                    }
                    observed.add(new PlaybackAttempt(candidate.url(), profile.name(), status, elapsed, null));

                    if (status < 200 || status >= 300) {
                        String reason = "Upstream stream returned HTTP " + status;
                        last = new IllegalStateException(reason);
                        observed.set(observed.size() - 1, new PlaybackAttempt(candidate.url(), profile.name(), status, elapsed, reason));
                        if (attempt == 1 && (status == 408 || status == 425 || status == 429 || status >= 500)) {
                            sleepQuietly(200L);
                            continue;
                        }
                        // Try the next browser/request profile before abandoning the candidate.
                        break;
                    }

                    String effectiveUrl = response.uri().toString();
                    byte[] body = response.body();
                    if (body.length > MAX_PROXY_RESPONSE_BYTES) {
                        throw new IllegalStateException("Upstream response exceeds the Live TV proxy safety limit");
                    }
                    boolean playlist = isHlsPlaylist(effectiveUrl, contentType, body);
                    if (playlist) {
                        String playlistText = new String(body, StandardCharsets.UTF_8);
                        playlistText = rewriteHlsPlaylist(channelId, effectiveUrl, playlistText);
                        body = playlistText.getBytes(StandardCharsets.UTF_8);
                        contentType = "application/vnd.apple.mpegurl";
                    } else if (contentType == null || contentType.isBlank()) {
                        contentType = contentTypeFor(effectiveUrl);
                    }
                    return new StreamProxyResponse(body, contentType, playlist, effectiveUrl);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("stream request interrupted", e);
                } catch (IOException | RuntimeException e) {
                    last = e;
                    String reason = safeErrorMessage(e, "upstream request failed");
                    observed.add(new PlaybackAttempt(candidate.url(), profile.name(), 0, elapsedMillis(started), reason));
                    if (attempt == 1 && isRetryable(e)) {
                        sleepQuietly(200L);
                        continue;
                    }
                    break;
                }
            }
        }
        throw new IllegalStateException(safeErrorMessage(last, "upstream request failed"));
    }

    private List<RequestProfile> requestProfiles(StreamCandidate candidate) {
        String supplied = candidate.userAgent();
        String chrome = supplied != null ? supplied : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36";
        return List.of(
                new RequestProfile("chrome", chrome, "application/vnd.apple.mpegurl, application/x-mpegURL, video/mp2t, audio/aac, */*", "en-US,en;q=0.9"),
                new RequestProfile("safari", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.6 Safari/605.1.15", "application/vnd.apple.mpegurl, application/x-mpegURL, */*", "en-US,en;q=0.8"),
                new RequestProfile("mobile", "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Mobile Safari/537.36", "application/vnd.apple.mpegurl, application/x-mpegURL, */*", "en-US,en;q=0.8"),
                new RequestProfile("vlc", "VLC/3.0.21 LibVLC/3.0.21", "*/*", "en-US,en;q=0.5"),
                new RequestProfile("ffmpeg", "Lavf/61.7.100", "*/*", "en-US,en;q=0.5")
        );
    }

    private record BrowserCompatibility(boolean compatible, long checkedAt) {}

    private record RequestProfile(String name, String userAgent, String accept, String acceptLanguage) {}

    private void recordPlaybackSuccess(LiveChannel channel, StreamCandidate candidate, StreamProxyResponse response) {
        PlaybackSnapshot snapshot = new PlaybackSnapshot(
                channel.getId(), channel.getTvgId(), channel.getName(), "PLAYING",
                "", candidate.url(), 200, Instant.now(), 1
        );
        playbackSnapshots.put(channel.getId(), snapshot);
        log.info("LIVE_TV_PLAYBACK_STARTED channelId={} tvgId={} name={} upstream={} status=200 effectiveUrl={} bytes={}",
                channel.getId(), channel.getTvgId(), channel.getName(), candidate.url(), response.effectiveUrl(), response.body().length);
    }

    private void recordPlaybackFailure(LiveChannel channel, List<PlaybackAttempt> attempts, PlaybackClassification classification) {
        String reason = attempts.isEmpty() ? "No upstream response" : attempts.get(attempts.size() - 1).reason();
        String message = classification == PlaybackClassification.UNAVAILABLE
                ? "Channel currently unavailable for free Live Streaming"
                : "Live stream could not be started. Please try again.";
        playbackSnapshots.put(channel.getId(), new PlaybackSnapshot(
                channel.getId(), channel.getTvgId(), channel.getName(), classification.name(), message,
                attempts.isEmpty() ? "" : attempts.get(attempts.size() - 1).url(),
                attempts.isEmpty() ? 0 : attempts.get(attempts.size() - 1).status(), Instant.now(), attempts.size()
        ));
        log.info("LIVE_TV_PLAYBACK_FAILED channelId={} tvgId={} name={} classification={} userMessage={} reason={} attempts={}",
                channel.getId(), channel.getTvgId(), channel.getName(), classification, message, reason, attempts.size());
    }

    private PlaybackClassification classifyFailures(List<StreamCandidate> candidates, List<PlaybackAttempt> attempts, List<String> failures) {
        if (candidates.isEmpty()) return PlaybackClassification.UNAVAILABLE;
        if (attempts.isEmpty()) return PlaybackClassification.ERROR;
        boolean allDefinitive = true;
        boolean sawResponse = false;
        for (PlaybackAttempt attempt : attempts) {
            if (attempt.status() > 0) sawResponse = true;
            int status = attempt.status();
            if (!(status == 404 || status == 410)) {
                allDefinitive = false;
            }
        }
        return sawResponse && allDefinitive ? PlaybackClassification.UNAVAILABLE : PlaybackClassification.ERROR;
    }

    public PlaybackDiagnostic getPlaybackDiagnostic(UUID channelId) {
        LiveChannel channel = channelRepository.findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Live channel not found"));
        List<StreamCandidate> candidates = streamCandidates(channel);
        List<PlaybackAttempt> attempts = new CopyOnWriteArrayList<>();
        List<String> failures = new ArrayList<>();
        int limit = Math.min(candidates.size(), MAX_PLAYBACK_ATTEMPTS_PER_DIAGNOSTIC);
        for (int i = 0; i < limit; i++) {
            StreamCandidate candidate = candidates.get(i);
            try {
                StreamProxyResponse response = fetchProxyResource(channelId, candidate, attempts);
                if (response != null) {
                    recordPlaybackSuccess(channel, candidate, response);
                    return new PlaybackDiagnostic(channelId, channel.getTvgId(), channel.getName(), "PLAYING",
                            "", "", candidate.url(), 200, attempts, Instant.now());
                }
            } catch (Exception e) {
                failures.add(safeErrorMessage(e, "upstream request failed"));
            }
        }
        PlaybackClassification classification = classifyFailures(candidates.subList(0, limit), attempts, failures);
        String message = classification == PlaybackClassification.UNAVAILABLE
                ? "Channel currently unavailable for free Live Streaming"
                : "Live stream could not be started. Please try again.";
        return new PlaybackDiagnostic(channelId, channel.getTvgId(), channel.getName(), classification.name(), message,
                failures.isEmpty() ? "No upstream response" : failures.get(0), "", 0, attempts, Instant.now());
    }

    public record PlaybackAttempt(String url, String profile, int status, int responseTimeMs, String reason) {}
    public record PlaybackDiagnostic(UUID channelId, String tvgId, String channelName, String classification,
                                     String userMessage, String mainReason, String successfulUrl, int status,
                                     List<PlaybackAttempt> attempts, Instant checkedAt) {}
    public record PlaybackSnapshot(UUID channelId, String tvgId, String channelName, String state, String userMessage,
                                   String upstreamUrl, int status, Instant timestamp, int attempts) {}
    private enum PlaybackClassification { PLAYING, UNAVAILABLE, ERROR }

    private boolean isRetryable(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("timed out") || message.contains("timeout") || message.contains("connection reset")
                || message.contains("connection refused") || message.contains("temporarily unavailable")
                || message.contains("http 5");
    }

    private void sleepQuietly(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static HttpClient buildIptvCompatibilityClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                public void checkServerTrusted(X509Certificate[] chain, String authType) { }
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            SSLParameters sslParameters = new SSLParameters();
            // IPTV compatibility is deliberately isolated to this outbound client.
            // The rest of the Spring/JVM HTTPS stack keeps normal certificate checks.
            sslParameters.setEndpointIdentificationAlgorithm("");
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .sslContext(sslContext)
                    .sslParameters(sslParameters)
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize IPTV compatibility HTTP client", e);
        }
    }

    private List<StreamCandidate> streamCandidates(LiveChannel channel) {
        Map<String, StreamCandidate> unique = new java.util.LinkedHashMap<>();
        String baseId = baseChannelId(channel.getTvgId());
        if (!baseId.isBlank()) {
            for (M3uEntry entry : localM3uByChannel.getOrDefault(baseId, List.of())) {
                String url = normalizeUrl(entry.url());
                if (url != null) unique.putIfAbsent(url.toLowerCase(Locale.ROOT), new StreamCandidate(url, entry.referrer(), entry.userAgent()));
            }
            for (JsonNode node : remoteStreamsByChannel.getOrDefault(baseId, List.of())) {
                String url = normalizeUrl(text(node, "url"));
                if (url != null) unique.putIfAbsent(url.toLowerCase(Locale.ROOT), new StreamCandidate(url, firstText(node, "referrer", "http_referrer"), text(node, "user_agent")));
            }
        }
        if (channel.getId() != null && unique.isEmpty()) {
            for (LiveStreamCandidate row : streamCandidateRepository.findAllByChannelIdOrderByIdAsc(channel.getId())) {
                String url = normalizeUrl(row.getUrl());
                if (url != null) unique.putIfAbsent(url.toLowerCase(Locale.ROOT), new StreamCandidate(url, row.getReferrer(), row.getUserAgent()));
            }
        }
        String primary = normalizeUrl(channel.getHlsUrl() != null && !channel.getHlsUrl().isBlank() ? channel.getHlsUrl() : channel.getUrl());
        if (primary != null) {
            StreamCandidate existing = unique.remove(primary.toLowerCase(Locale.ROOT));
            java.util.LinkedHashMap<String, StreamCandidate> ordered = new java.util.LinkedHashMap<>();
            ordered.put(primary.toLowerCase(Locale.ROOT), existing != null ? existing : new StreamCandidate(primary, null, null));
            ordered.putAll(unique);
            return new ArrayList<>(ordered.values());
        }
        return new ArrayList<>(unique.values());
    }

    private StreamCandidate candidateForUrl(LiveChannel channel, String url) {
        StreamCandidate local = streamCandidates(channel).stream()
                .filter(candidate -> candidate.url().equalsIgnoreCase(url))
                .findFirst()
                .orElse(null);
        if (local != null && (local.referrer() != null || local.userAgent() != null)) {
            return local;
        }

        StreamCandidate remote = streamHeadersByUrl.get(url);
        if (remote != null) {
            return remote;
        }

        // Child HLS URLs are normally not present in IPTV-org's stream catalogue.
        // Fall back to the exact feed that successfully served the manifest so its
        // required headers continue to be applied to every playlist/segment/key.
        if (channel != null && channel.getId() != null) {
            StreamCandidate selected = selectedProxyCandidates.get(channel.getId());
            if (selected != null) {
                return selected;
            }
        }

        return local;
    }

    private record StreamCandidate(String url, String referrer, String userAgent) {}

    public record StreamCandidateInfo(String url, String proxyUrl, String feedId, String quality, String status, boolean directBrowserCompatible) {}

    @Transactional(readOnly = true)
    public List<StreamCandidateInfo> getStreamCandidateInfo(UUID channelId) {
        LiveChannel channel = channelRepository.findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Live channel not found"));
        List<StreamCandidateInfo> result = new ArrayList<>();
        Map<String, LiveStreamCandidate> persisted = new java.util.LinkedHashMap<>();
        if (channel.getId() != null) {
            for (LiveStreamCandidate row : streamCandidateRepository.findAllByChannelIdOrderByIdAsc(channel.getId())) {
                persisted.put(row.getUrl().toLowerCase(Locale.ROOT), row);
            }
        }
        for (StreamCandidate candidate : streamCandidates(channel)) {
            LiveStreamCandidate row = persisted.get(candidate.url().toLowerCase(Locale.ROOT));
            String feed = row == null ? "" : (row.getFeedId() == null ? "" : row.getFeedId());
            String quality = row == null ? "" : (row.getQuality() == null ? "" : row.getQuality());
            String status = row == null || row.getStatus() == null ? "unknown" : row.getStatus().toJson();
            String proxy = "/api/v1/live/channels/" + channelId + "/stream.m3u8?url=" + urlEncode(candidate.url());
            boolean directCompatible = isDirectBrowserCompatible(candidate);
            result.add(new StreamCandidateInfo(candidate.url(), proxy, feed, quality, status, directCompatible));
        }
        return result;
    }

    /**
     * Checks whether a provider explicitly permits browser HLS requests from the configured web origin.
     * A positive result is required before the React player uses the provider URL directly. This prevents
     * noisy CORS failures for normal IPTV sources that must be relayed through our compatibility proxy.
     */
    private boolean isDirectBrowserCompatible(StreamCandidate candidate) {
        String url = normalizeUrl(candidate.url());
        if (url == null || !isHlsPlaylistUrl(url)) return false;
        long now = System.currentTimeMillis();
        BrowserCompatibility cached = browserCompatibilityByUrl.get(url.toLowerCase(Locale.ROOT));
        if (cached != null && now - cached.checkedAt() < BROWSER_COMPATIBILITY_CACHE_MS) {
            return cached.compatible();
        }
        boolean compatible = false;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(8))
                    .header("Origin", normalizeHeader(browserOrigin) != null ? normalizeHeader(browserOrigin) : "http://localhost:3000")
                    .header("Accept", "application/vnd.apple.mpegurl, application/x-mpegURL, */*")
                    .header("User-Agent", candidate.userAgent() != null ? candidate.userAgent() : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36")
                    .header("Range", "bytes=0-4095");
            if (candidate.referrer() != null && !candidate.referrer().isBlank()) {
                builder.header("Referer", candidate.referrer());
            }
            HttpResponse<InputStream> response = iptvCompatibilityClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                body.readNBytes(4096);
            }
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String allowOrigin = response.headers().firstValue("Access-Control-Allow-Origin").orElse("").trim();
                String configuredOrigin = normalizeHeader(browserOrigin);
                compatible = "*".equals(allowOrigin)
                        || (configuredOrigin != null && configuredOrigin.equalsIgnoreCase(allowOrigin));
            }
        } catch (Exception ignored) {
            compatible = false;
        }
        browserCompatibilityByUrl.put(url.toLowerCase(Locale.ROOT), new BrowserCompatibility(compatible, now));
        return compatible;
    }

    private boolean isHlsPlaylistUrl(String url) {
        return isHlsPlaylist(url, "", null);
    }

    private String rewriteHlsPlaylist(UUID channelId, String baseUrl, String playlist) {
        if (playlist == null || playlist.isBlank()) {
            return playlist;
        }

        StringBuilder out = new StringBuilder(playlist.length() + 512);
        String[] lines = playlist.split("\\R", -1);
        String proxyBase = "/api/v1/live/channels/" + channelId + "/stream.m3u8";

        for (String line : lines) {
            String rewritten = line;

            // URI="..." appears in EXT-X-KEY, EXT-X-MAP, EXT-X-MEDIA and
            // similar tags. Those resources must also remain inside the proxy.
            java.util.regex.Matcher uriMatcher = java.util.regex.Pattern
                    .compile("(?i)(URI=\\\")([^\\\"]+)(\\\")")
                    .matcher(rewritten);
            StringBuffer tagBuffer = new StringBuffer();
            while (uriMatcher.find()) {
                String absolute = resolveHlsUrl(baseUrl, uriMatcher.group(2));
                String proxied = proxyBase + "?url=" + urlEncode(absolute);
                uriMatcher.appendReplacement(tagBuffer,
                        java.util.regex.Matcher.quoteReplacement(uriMatcher.group(1) + proxied + uriMatcher.group(3)));
            }
            uriMatcher.appendTail(tagBuffer);
            rewritten = tagBuffer.toString();

            // Playlist/segment URI lines are normally plain relative or absolute
            // URLs. Preserve comments and empty lines untouched.
            String trimmed = rewritten.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                String absolute = resolveHlsUrl(baseUrl, trimmed);
                rewritten = proxyBase + "?url=" + urlEncode(absolute);
            }

            out.append(rewritten).append('\n');
        }

        return out.toString();
    }

    private String resolveHlsUrl(String baseUrl, String value) {
        try {
            return URI.create(baseUrl).resolve(value).toString();
        } catch (Exception e) {
            return value;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isHlsPlaylist(String url, String contentType, byte[] body) {
        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        String lowerType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (lowerUrl.contains(".m3u8") || lowerUrl.contains("m3u8")) {
            return true;
        }
        if (lowerType.contains("mpegurl") || lowerType.contains("vnd.apple.mpegurl")) {
            return true;
        }
        if (body != null && body.length >= 7) {
            String prefix = new String(body, 0, Math.min(body.length, 32), StandardCharsets.UTF_8).trim();
            return prefix.startsWith("#EXTM3U");
        }
        return false;
    }

    private String contentTypeFor(String url) {
        String lower = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (lower.contains(".m3u8") || lower.contains("m3u8")) {
            return "application/vnd.apple.mpegurl";
        }
        if (lower.contains(".ts")) {
            return "video/mp2t";
        }
        if (lower.contains(".aac")) {
            return "audio/aac";
        }
        if (lower.contains(".key")) {
            return "application/octet-stream";
        }
        return "application/octet-stream";
    }

    private void validateProxyTarget(String targetUrl) {
        try {
            URI uri = URI.create(targetUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Only HTTP and HTTPS streams are supported");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Stream host is missing");
            }

            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new IllegalArgumentException("Private/local stream targets are not allowed");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Stream host could not be resolved", e);
        }
    }

    public record StreamProxyResponse(
            byte[] body,
            String contentType,
            boolean playlist,
            String effectiveUrl) {
    }

    private void rebuildSearchIndex() {
        try {
            searchIndex.rebuild(channelRepository.findAllByDeletedFalse());
            log.info("Live TV search index rebuilt: channels={}", channelRepository.countByDeletedFalse());
        } catch (Exception e) {
            log.warn("Unable to rebuild Live TV search index: {}", e.getMessage());
        }
    }

    private Page<LiveChannel> fastSearch(String country, String category, String search, int page, int size, Sort sort) {
        if (!searchIndex.isBuilt()) rebuildSearchIndex();
        Set<UUID> ids = searchIndex.matchingIds(search);
        if (ids.isEmpty()) return Page.empty(PageRequest.of(page, size, sort));

        List<LiveChannel> rows = new ArrayList<>(channelRepository.findAllById(ids));
        rows.removeIf(c -> c.isDeleted()
                || (country != null && !country.equalsIgnoreCase(c.getCountryCode()))
                || (category != null && (c.getCategories() == null || c.getCategories().stream().noneMatch(v -> category.equalsIgnoreCase(v)))));

        java.util.Comparator<LiveChannel> comparator = comparatorFor(sort);
        rows.sort(comparator);
        long total = rows.size();
        int from = Math.min(page * size, rows.size());
        int to = Math.min(from + size, rows.size());
        List<LiveChannel> content = from >= to ? List.of() : new ArrayList<>(rows.subList(from, to));
        return new PageImpl<>(content, PageRequest.of(page, size, sort), total);
    }

    private java.util.Comparator<LiveChannel> comparatorFor(Sort sort) {
        Sort.Order order = sort.stream().findFirst().orElse(Sort.Order.asc("name"));
        java.util.Comparator<LiveChannel> c;
        c = switch (order.getProperty()) {
            case "country" -> java.util.Comparator.comparing(x -> safeSort(x.getCountry()), String.CASE_INSENSITIVE_ORDER);
            case "category" -> java.util.Comparator.comparing(x -> safeSort(x.getCategory()), String.CASE_INSENSITIVE_ORDER);
            case "status" -> java.util.Comparator.comparing(x -> x.getStatus() == null ? "" : x.getStatus().name());
            case "viewCount" -> java.util.Comparator.comparingLong(LiveChannel::getViewCount);
            case "lastChecked" -> java.util.Comparator.comparing(x -> x.getLastChecked() == null ? java.time.Instant.EPOCH : x.getLastChecked());
            default -> java.util.Comparator.comparing(x -> safeSort(x.getName()), String.CASE_INSENSITIVE_ORDER);
        };
        return order.isDescending() ? c.reversed().thenComparing(x -> safeSort(x.getName()), String.CASE_INSENSITIVE_ORDER)
                : c.thenComparing(x -> safeSort(x.getName()), String.CASE_INSENSITIVE_ORDER);
    }

    private String safeSort(String value) { return value == null ? "" : value; }

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

        Page<LiveChannel> result;
        if (normalizedSearch != null) {
            result = fastSearch(normalizedCountry, normalizedCategory, normalizedSearch, safePage, safeSize, parseSort(sort));
        } else {
            result = channelRepository.findWithFilters(
                    normalizedCountry, normalizedCategory, null, pageable
            );
        }

        applyUserFavorites(result.getContent());
        return result;
    }

    @Cacheable(
            value = "liveCountries",
            key = "#p0 + ':' + #p1"
    )
    @Transactional(readOnly = true)
    public List<CountryStats> getCountries(String category, String search) {
        String normalizedCategory = normalizeFilter(category, false);
        String normalizedSearch = normalizeFilter(search, false);
        if (normalizedSearch != null) {
            if (!searchIndex.isBuilt()) rebuildSearchIndex();
            Set<UUID> ids = searchIndex.matchingIds(normalizedSearch);
            if (ids.isEmpty()) return List.of();
            Map<String, long[]> counts = new HashMap<>();
            for (LiveChannel c : channelRepository.findAllById(ids)) {
                if (c.isDeleted() || (normalizedCategory != null && (c.getCategories() == null || c.getCategories().stream().noneMatch(v -> normalizedCategory.equalsIgnoreCase(v))))) continue;
                counts.computeIfAbsent(c.getCountryCode(), k -> new long[]{0, 0})[0]++;
                counts.get(c.getCountryCode())[1] = 1;
            }
            Map<String, String> names = new HashMap<>();
            for (LiveChannel c : channelRepository.findAllById(ids)) if (!c.isDeleted()) names.putIfAbsent(c.getCountryCode(), c.getCountry());
            return counts.entrySet().stream().map(e -> new CountryStats(e.getKey(), names.getOrDefault(e.getKey(), e.getKey()), e.getValue()[0]))
                    .sorted(java.util.Comparator.comparingLong(CountryStats::getChannelCount).reversed().thenComparing(CountryStats::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        }
        return channelRepository.getCountryStats(normalizedCategory, null);
    }

    @Cacheable(
            value = "liveCategories",
            key = "#p0 + ':' + #p1"
    )
    @Transactional(readOnly = true)
    public List<CategoryStats> getCategories(String country, String search) {
        String normalizedCountry = normalizeFilter(country, true);
        String normalizedSearch = normalizeFilter(search, false);
        if (normalizedSearch != null) {
            if (!searchIndex.isBuilt()) rebuildSearchIndex();
            Set<UUID> ids = searchIndex.matchingIds(normalizedSearch);
            Map<String, Long> counts = new HashMap<>();
            for (LiveChannel c : channelRepository.findAllById(ids)) {
                if (c.isDeleted() || (normalizedCountry != null && !normalizedCountry.equalsIgnoreCase(c.getCountryCode()))) continue;
                if (c.getCategories() != null && !c.getCategories().isEmpty()) {
                    for (String category : c.getCategories()) counts.merge(category, 1L, Long::sum);
                } else {
                    counts.merge(c.getCategory(), 1L, Long::sum);
                }
            }
            return counts.entrySet().stream().map(e -> new CategoryStats(e.getKey(), e.getValue()))
                    .sorted(java.util.Comparator.comparingLong(CategoryStats::getChannelCount).reversed().thenComparing(CategoryStats::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        }
        return channelRepository.getCategoryStats(normalizedCountry, null);
    }

    @Cacheable(value = "liveChannel", key = "#p0")
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
        UUID userId = currentUserAccessor.getCurrentUserId();
        LiveChannel channel = channelRepository.findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));

        if (userFavoriteRepository.exists(userId, channelId)) {
            userFavoriteRepository.remove(userId, channelId);
            channel.setFavorite(false);
        } else {
            userFavoriteRepository.add(userId, channelId);
            channel.setFavorite(true);
        }
        return channel;
    }

    private void applyUserFavorites(List<LiveChannel> channels) {
        if (channels == null || channels.isEmpty()) return;
        UUID userId;
        try {
            userId = currentUserAccessor.getCurrentUserId();
        } catch (IllegalStateException unauthenticated) {
            channels.forEach(c -> c.setFavorite(false));
            return;
        }
        List<UUID> ids = channels.stream().map(LiveChannel::getId).filter(java.util.Objects::nonNull).toList();
        Set<UUID> favoriteIds = new HashSet<>(userFavoriteRepository.findFavoriteChannelIds(userId, ids));
        channels.forEach(c -> c.setFavorite(favoriteIds.contains(c.getId())));
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
        List<StreamCandidate> candidates = streamCandidates(channel);
        boolean healthy = false;
        for (StreamCandidate candidate : candidates) {
            if (checkStreamHealth(candidate.url(), candidate.referrer(), candidate.userAgent())) { healthy = true; break; }
        }
        int responseMs = elapsedMillis(started);
        if (healthy) channel.markOnline(responseMs);
        else if (!candidates.isEmpty()) { channel.setStatus(LiveChannel.StreamStatus.DEGRADED); channel.setLastChecked(Instant.now()); channel.setCheckError("No indexed candidate responded from this server"); }
        else channel.markOffline("No stream candidates are currently indexed");

        LiveChannel saved = saveWithOptimisticLockingRetry(channel);
        clearCaches();

        return saved;
    }

    @Transactional(readOnly = true)
    public LogoResponse getLogo(UUID channelId) {
        LiveChannel channel = channelRepository.findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Live channel not found"));
        String key = channel.getTvgId() == null ? "" : channel.getTvgId().trim().toLowerCase(Locale.ROOT);
        Path local = localLogoFiles.get(key);
        if (local == null) {
            Path root = resolveLocalRepoRoot();
            if (root != null) {
                for (String ext : List.of("png", "jpg", "jpeg", "webp", "svg", "gif", "avif")) {
                    Path candidate = root.resolve("logos").resolve(channel.getTvgId() + "." + ext).normalize();
                    if (Files.isRegularFile(candidate)) { local = candidate; break; }
                }
            }
        }
        try {
            if (local != null && Files.isRegularFile(local)) {
                return new LogoResponse(Files.readAllBytes(local), logoContentType(local.getFileName().toString()));
            }
            String remote = normalizeUrl(channelToLogo.get(channel.getTvgId()));
            if (remote == null) remote = normalizeUrl(channel.getLogo());
            if (remote == null) throw new IllegalStateException("No IPTV-org logo is available for " + channel.getTvgId());
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(remote)).GET().timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/154 Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8").build();
            HttpResponse<byte[]> response = iptvCompatibilityClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Logo upstream HTTP " + response.statusCode());
            String type = response.headers().firstValue("Content-Type").orElse(logoContentType(remote));
            return new LogoResponse(response.body(), type);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Logo request interrupted", e);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(safeErrorMessage(e, "Logo unavailable"), e);
        }
    }

    private String logoContentType(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".svg") || lower.contains("svg")) return "image/svg+xml";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".avif")) return "image/avif";
        return "image/png";
    }

    public record LogoResponse(byte[] body, String contentType) {}

    @Transactional(readOnly = true)
    public List<LiveChannel> getFeatured() {
        List<LiveChannel> featured = channelRepository.findTopOnlineChannels(
                PageRequest.of(0, DEFAULT_FEATURED_SIZE)
        );
        applyUserFavorites(featured);
        return featured;
    }

    @Transactional
    public LiveChannel recordView(UUID channelId) {
        LiveChannel channel = channelRepository
                .findByIdAndDeletedFalse(channelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Channel not found: " + channelId
                ));

        channel.incrementViewCount();

        return saveWithOptimisticLockingRetry(channel);
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

package com.platform.livetv.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "live_channels", schema = "live_tv_schema",
        indexes = {
                @Index(name = "idx_live_channels_country", columnList = "country_code"),
                @Index(name = "idx_live_channels_category", columnList = "category"),
                @Index(name = "idx_live_channels_language", columnList = "language"),
                @Index(name = "idx_live_channels_status", columnList = "status"),
                @Index(name = "idx_live_channels_name", columnList = "name"),
                @Index(name = "idx_live_channels_tvg_id", columnList = "tvg_id"),
                @Index(name = "idx_live_channels_favorite", columnList = "is_favorite"),
                @Index(name = "idx_live_channels_deleted", columnList = "deleted"),
                @Index(name = "idx_live_channels_deleted_country", columnList = "deleted, country_code"),
                @Index(name = "idx_live_channels_deleted_category", columnList = "deleted, category"),
                @Index(name = "idx_live_channels_deleted_name", columnList = "deleted, name")
        })
public class LiveChannel extends Auditable implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /** All IPTV-org category memberships. `category` remains the primary display category. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "live_channel_categories",
            schema = "live_tv_schema",
            joinColumns = @JoinColumn(name = "channel_id", nullable = false),
            indexes = {
                    @Index(name = "idx_live_channel_categories_category", columnList = "category"),
                    @Index(name = "idx_live_channel_categories_channel", columnList = "channel_id")
            }
    )
    @Column(name = "category", nullable = false, length = 100)
    private Set<String> categories = new LinkedHashSet<>();

    @Column(name = "logo", columnDefinition = "TEXT")
    private String logo;

    @Column(name = "url", length = 4096)
    private String url;

    @Column(name = "hls_url", length = 4096)
    private String hlsUrl;

    @Column(name = "is_nsfw", nullable = false)
    @com.fasterxml.jackson.annotation.JsonProperty("isNsfw")
    private boolean nsfw = false;

    @Column(name = "tvg_id", length = 255)
    private String tvgId;

    @Column(name = "tvg_name", length = 255)
    private String tvgName;

    @Column(name = "tvg_logo", columnDefinition = "TEXT")
    private String tvgLogo;

    @Column(name = "group_title", length = 255)
    private String groupTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StreamStatus status = StreamStatus.UNKNOWN;

    @Column(name = "last_checked")
    private Instant lastChecked;

    @Column(name = "check_error", length = 1000)
    private String checkError;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite = false;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "stream_candidate_count", nullable = false)
    private int streamCandidateCount = 0;

    @Column(name = "epg_url", columnDefinition = "TEXT")
    private String epgUrl;

    public enum StreamStatus {
        PLAYABLE, DEGRADED, NO_SOURCE, CHECKING, UNKNOWN;

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public void markOnline(int responseTimeMs) {
        this.status = StreamStatus.PLAYABLE;
        this.lastChecked = Instant.now();
        this.responseTimeMs = responseTimeMs;
        this.checkError = null;
    }

    public void markOffline(String error) {
        this.status = StreamStatus.NO_SOURCE;
        this.lastChecked = Instant.now();
        this.checkError = error;
        this.responseTimeMs = null;
    }

    public void markChecking() {
        this.status = StreamStatus.CHECKING;
    }

    /**
     * Best-effort classification of the actual stream URL.
     *
     * IPTV-org exposes publicly available stream links but does not publish a
     * reliable paid/subscription flag. Therefore the API deliberately reports
     * PUBLIC vs PROTECTED rather than pretending it can prove a broadcaster's
     * commercial subscription status. URLs containing explicit credentials or
     * common Xtream-style credential paths are treated as protected.
     */
    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("accessType")
    public String getAccessType() {
        String stream = hlsUrl != null && !hlsUrl.isBlank() ? hlsUrl : url;
        if (stream == null || stream.isBlank()) {
            return "PROTECTED";
        }

        String normalized = stream.trim().toLowerCase(Locale.ROOT);
        boolean hasExplicitCredentials = normalized.matches(".*[?&](username|user|password|pass)=[^&]+.*")
                || normalized.matches(".*[?&](authorization|auth)=.+.*")
                || normalized.matches(".*/live/[^/]+/[^/]+(?:/|\\?).*");

        return hasExplicitCredentials ? "PROTECTED" : "PUBLIC";
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void toggleFavorite() {
        this.favorite = !this.favorite;
    }
    @Override
    @Transient
    public boolean isNew() {
        return id == null;
    }

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String v) { this.countryCode = v; }
    public String getLanguage() { return language; }
    public void setLanguage(String v) { this.language = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public Set<String> getCategories() { return categories; }
    public void setCategories(Set<String> v) { this.categories = v == null ? new LinkedHashSet<>() : new LinkedHashSet<>(v); }
    public String getLogo() { return logo; }
    public void setLogo(String v) { this.logo = v; }
    public String getUrl() { return url; }
    public void setUrl(String v) { this.url = v; }
    public String getHlsUrl() { return hlsUrl; }
    public void setHlsUrl(String v) { this.hlsUrl = v; }
    public boolean isNsfw() { return nsfw; }
    public void setNsfw(boolean v) { this.nsfw = v; }
    public String getTvgId() { return tvgId; }
    public void setTvgId(String v) { this.tvgId = v; }
    public String getTvgName() { return tvgName; }
    public void setTvgName(String v) { this.tvgName = v; }
    public String getTvgLogo() { return tvgLogo; }
    public void setTvgLogo(String v) { this.tvgLogo = v; }
    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String v) { this.groupTitle = v; }
    public StreamStatus getStatus() { return status; }
    public void setStatus(StreamStatus v) { this.status = v; }
    public Instant getLastChecked() { return lastChecked; }
    public void setLastChecked(Instant v) { this.lastChecked = v; }
    public String getCheckError() { return checkError; }
    public void setCheckError(String v) { this.checkError = v; }
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer v) { this.responseTimeMs = v; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean v) { this.favorite = v; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long v) { this.viewCount = v; }
    public int getStreamCandidateCount() { return streamCandidateCount; }
    public void setStreamCandidateCount(int v) { this.streamCandidateCount = v; }
    public String getEpgUrl() { return epgUrl; }
    public void setEpgUrl(String v) { this.epgUrl = v; }

}

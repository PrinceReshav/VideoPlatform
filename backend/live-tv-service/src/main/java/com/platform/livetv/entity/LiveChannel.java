package com.platform.livetv.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

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
                @Index(name = "idx_live_channels_deleted", columnList = "deleted")
        })
@Getter
@Setter
public class LiveChannel extends Auditable {

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

    @Column(name = "logo", length = 500)
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

    @Column(name = "tvg_logo", length = 500)
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

    @Column(name = "epg_url", length = 500)
    private String epgUrl;

    public enum StreamStatus {
        ONLINE, OFFLINE, CHECKING, UNKNOWN;

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public void markOnline(int responseTimeMs) {
        this.status = StreamStatus.ONLINE;
        this.lastChecked = Instant.now();
        this.responseTimeMs = responseTimeMs;
        this.checkError = null;
    }

    public void markOffline(String error) {
        this.status = StreamStatus.OFFLINE;
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
}
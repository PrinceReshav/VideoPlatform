package com.platform.livetv.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "live_stream_candidates", schema = "live_tv_schema",
        uniqueConstraints = @UniqueConstraint(name = "uq_live_stream_candidate", columnNames = {"channel_id", "url"}),
        indexes = {
                @Index(name = "idx_live_stream_candidates_channel", columnList = "channel_id"),
                @Index(name = "idx_live_stream_candidates_status", columnList = "status"),
                @Index(name = "idx_live_stream_candidates_feed", columnList = "feed_id")
        })
public class LiveStreamCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "feed_id", length = 255)
    private String feedId;

    @Column(name = "url", nullable = false, length = 4096)
    private String url;

    @Column(name = "quality", length = 100)
    private String quality;

    @Column(name = "labels", length = 2000)
    private String labels;

    @Column(name = "referrer", length = 4096)
    private String referrer;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LiveChannel.StreamStatus status = LiveChannel.StreamStatus.UNKNOWN;

    @Column(name = "last_checked")
    private Instant lastChecked;

    @Column(name = "check_error", length = 1000)
    private String checkError;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;
    public UUID getId() { return id; }
    public void setId(UUID v) { id = v; }
    public UUID getChannelId() { return channelId; }
    public void setChannelId(UUID v) { channelId = v; }
    public String getFeedId() { return feedId; }
    public void setFeedId(String v) { feedId = v; }
    public String getUrl() { return url; }
    public void setUrl(String v) { url = v; }
    public String getQuality() { return quality; }
    public void setQuality(String v) { quality = v; }
    public String getLabels() { return labels; }
    public void setLabels(String v) { labels = v; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String v) { referrer = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { userAgent = v; }
    public LiveChannel.StreamStatus getStatus() { return status; }
    public void setStatus(LiveChannel.StreamStatus v) { status = v; }
    public Instant getLastChecked() { return lastChecked; }
    public void setLastChecked(Instant v) { lastChecked = v; }
    public String getCheckError() { return checkError; }
    public void setCheckError(String v) { checkError = v; }
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer v) { responseTimeMs = v; }

}

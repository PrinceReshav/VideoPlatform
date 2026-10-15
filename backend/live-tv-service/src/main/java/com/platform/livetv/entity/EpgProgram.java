package com.platform.livetv.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "epg_programs", schema = "live_tv_schema", indexes = {
        @Index(name = "idx_epg_programs_channel_time", columnList = "channel_id,start_time,end_time"),
        @Index(name = "idx_epg_programs_time", columnList = "start_time,end_time")
})
public class EpgProgram extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "channel_id", nullable = false)
    private UUID channelId;
    @Column(nullable = false, length = 500)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "start_time", nullable = false)
    private Instant startTime;
    @Column(name = "end_time", nullable = false)
    private Instant endTime;
    @Column(length = 100)
    private String category;
    @Column(length = 500)
    private String icon;
    private Integer season;
    private Integer episode;
    @Column(name = "episode_title", length = 500)
    private String episodeTitle;
    @Column(nullable = false)
    private boolean deleted = false;

    public UUID getId() { return id; }
    public UUID getChannelId() { return channelId; }
    public void setChannelId(UUID v) { channelId = v; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public String getCategory() { return category; }
    public String getIcon() { return icon; }
    public Integer getSeason() { return season; }
    public Integer getEpisode() { return episode; }
    public String getEpisodeTitle() { return episodeTitle; }
}

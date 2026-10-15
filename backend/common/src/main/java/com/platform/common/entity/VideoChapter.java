package com.platform.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Video chapter entity for Feature 12: Video Chapters
 */
@Entity
@Table(name = "video_chapters", schema = "video_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE video_chapters SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class VideoChapter {

    @Id
    @GeneratedValue
    private UUID id;

    /** The video this chapter belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    /** The title of the chapter */
    @Column(length = 200, nullable = false)
    private String title;

    /** The start time of the chapter in seconds */
    @Column(nullable = false)
    private Integer startTimeSeconds;

    /** The end time of the chapter in seconds */
    private Integer endTimeSeconds;

    /** A custom thumbnail URL for the chapter (optional) */
    @Column(length = 500)
    private String thumbnailUrl;

    /** Description or summary of the chapter */
    @Column(length = 1000)
    private String description;

    /** Order/position of the chapter in the video */
    @Column(nullable = false)
    private Integer position;

    /** Timestamp when the chapter was created */
    private Instant createdAt;

    /** Timestamp when the chapter was last updated */
    private Instant updatedAt;

    /** Soft delete flag */
    private boolean deleted = false;
}
package com.platform.common.service;

import com.platform.common.entity.Video;
import com.platform.common.entity.VideoChapter;
import com.platform.common.repository.VideoChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling video chapters.
 *
 * Feature 12: Video Chapters
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoChapterService {

    private final VideoChapterRepository videoChapterRepository;

    /**
     * Create a new video chapter for a video.
     */
    @Transactional
    public VideoChapter createVideoChapter(
            UUID videoId,
            VideoChapter chapter) {

        if (videoId == null) {
            throw new IllegalArgumentException("Video ID cannot be null");
        }

        if (chapter == null) {
            throw new IllegalArgumentException("Video chapter cannot be null");
        }

        /*
         * Attach the existing video by ID.
         *
         * This avoids loading the complete Video entity merely to
         * establish the relationship.
         */
        Video video = new Video();
        video.setId(videoId);
        chapter.setVideo(video);

        Instant now = Instant.now();

        chapter.setCreatedAt(now);
        chapter.setUpdatedAt(now);

        VideoChapter savedChapter =
                videoChapterRepository.save(chapter);

        log.info(
                "Created video chapter {} for video {} at position {}",
                savedChapter.getId(),
                savedChapter.getVideo().getId(),
                savedChapter.getPosition()
        );

        return savedChapter;
    }

    /**
     * Get a video chapter by ID.
     */
    @Transactional(readOnly = true)
    public VideoChapter getVideoChapterById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Video chapter ID cannot be null"
            );
        }

        return videoChapterRepository.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Video chapter not found with ID: " + id
                        )
                );
    }

    /**
     * Get all video chapters for a video.
     */
    @Transactional(readOnly = true)
    public List<VideoChapter> getVideoChaptersByVideoId(
            UUID videoId) {

        if (videoId == null) {
            throw new IllegalArgumentException(
                    "Video ID cannot be null"
            );
        }

        return videoChapterRepository
                .findByVideoIdOrderByPositionAsc(videoId);
    }

    /**
     * Get video chapters by creation timestamp range.
     */
    @Transactional(readOnly = true)
    public List<VideoChapter> getVideoChaptersByTimestampRange(
            Instant startTime,
            Instant endTime) {

        if (startTime != null
                && endTime != null
                && startTime.isAfter(endTime)) {

            throw new IllegalArgumentException(
                    "Start time cannot be after end time"
            );
        }

        return videoChapterRepository.findByCreatedAtBetween(
                startTime,
                endTime
        );
    }

    /**
     * Update a video chapter.
     *
     * Immutable fields such as ID, video and createdAt are preserved.
     */
    @Transactional
    public VideoChapter updateVideoChapter(
            UUID id,
            VideoChapter chapterDetails) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Video chapter ID cannot be null"
            );
        }

        if (chapterDetails == null) {
            throw new IllegalArgumentException(
                    "Video chapter details cannot be null"
            );
        }

        VideoChapter chapter =
                getVideoChapterById(id);

        chapter.setTitle(
                chapterDetails.getTitle()
        );

        chapter.setStartTimeSeconds(
                chapterDetails.getStartTimeSeconds()
        );

        chapter.setEndTimeSeconds(
                chapterDetails.getEndTimeSeconds()
        );

        chapter.setThumbnailUrl(
                chapterDetails.getThumbnailUrl()
        );

        chapter.setDescription(
                chapterDetails.getDescription()
        );

        chapter.setPosition(
                chapterDetails.getPosition()
        );

        chapter.setUpdatedAt(
                Instant.now()
        );

        VideoChapter updatedChapter =
                videoChapterRepository.save(chapter);

        log.info(
                "Updated video chapter {}",
                updatedChapter.getId()
        );

        return updatedChapter;
    }

    /**
     * Delete a video chapter.
     */
    @Transactional
    public void deleteVideoChapter(UUID id) {

        VideoChapter chapter =
                getVideoChapterById(id);

        videoChapterRepository.delete(chapter);

        log.info(
                "Deleted video chapter {}",
                id
        );
    }

    /**
     * Reorder video chapters for a video.
     *
     * The supplied list defines the new order:
     *
     * index 0 -> position 0
     * index 1 -> position 1
     * index 2 -> position 2
     * etc.
     */
    @Transactional
    public void reorderVideoChapters(
            UUID videoId,
            List<UUID> chapterIdsInOrder) {

        if (videoId == null) {
            throw new IllegalArgumentException(
                    "Video ID cannot be null"
            );
        }

        if (chapterIdsInOrder == null) {
            throw new IllegalArgumentException(
                    "Chapter ID list cannot be null"
            );
        }

        List<VideoChapter> chapters =
                videoChapterRepository.findByVideoId(videoId);

        /*
         * IMPORTANT:
         *
         * Do not use a lambda that captures the loop variable `i`.
         *
         * Java requires variables captured by lambdas to be final
         * or effectively final. Since `i` changes on every iteration,
         * the original code caused:
         *
         * "local variables referenced from a lambda expression
         * must be final or effectively final"
         *
         * A normal nested loop avoids that problem and also makes
         * validation of chapter ownership explicit.
         */

        Instant now = Instant.now();

        for (int position = 0;
             position < chapterIdsInOrder.size();
             position++) {

            UUID chapterId =
                    chapterIdsInOrder.get(position);

            if (chapterId == null) {
                continue;
            }

            VideoChapter matchingChapter = null;

            for (VideoChapter chapter : chapters) {

                if (chapterId.equals(chapter.getId())) {
                    matchingChapter = chapter;
                    break;
                }
            }

            /*
             * Do not silently reorder a chapter belonging to another
             * video. The requested chapter must belong to this video.
             */
            if (matchingChapter == null) {
                throw new IllegalArgumentException(
                        "Chapter " + chapterId
                                + " does not belong to video "
                                + videoId
                );
            }

            matchingChapter.setPosition(position);
            matchingChapter.setUpdatedAt(now);
        }

        videoChapterRepository.saveAll(chapters);

        log.info(
                "Reordered {} video chapters for video {}",
                chapterIdsInOrder.size(),
                videoId
        );
    }

    /**
     * Get video chapters with filtering.
     *
     * The repository returns a Page, while this service exposes
     * the existing List-based service contract.
     */
    @Transactional(readOnly = true)
    public List<VideoChapter> getVideoChaptersByCriteria(
            UUID videoId,
            Instant startTime,
            Instant endTime) {

        if (startTime != null
                && endTime != null
                && startTime.isAfter(endTime)) {

            throw new IllegalArgumentException(
                    "Start time cannot be after end time"
            );
        }

        return videoChapterRepository.findByCriteria(
                videoId,
                startTime,
                endTime,
                PageRequest.of(
                        0,
                        Integer.MAX_VALUE
                )
        ).getContent();
    }
}
package com.platform.video.repository;

import com.platform.common.entity.ProcessingJob;
import com.platform.common.entity.Video;
import com.platform.common.entity.VideoFile;
import com.platform.video.projection.VideoListProjection;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface VideoRepository extends com.platform.common.repository.VideoRepository {

    Optional<Video> findBySlug(String slug);

    List<Video> findByCreatorId(UUID creatorId);

    List<Video> findByChannelId(UUID channelId);

    @Query("SELECT v FROM Video v WHERE v.creatorId = :creatorId AND v.deleted = false ORDER BY v.createdAt DESC")
    List<Video> findByCreatorIdAndDeletedFalse(@Param("creatorId") UUID creatorId);

    @Query("SELECT v FROM Video v WHERE v.channelId = :channelId AND v.deleted = false ORDER BY v.publishedAt DESC")
    List<Video> findByChannelIdAndDeletedFalse(@Param("channelId") UUID channelId);

    @Query("SELECT v FROM Video v WHERE v.publicationState = 'PUBLISHED' AND v.processingState = 'READY' AND v.blocked = false AND v.deleted = false AND v.visibility = 'PUBLIC' ORDER BY v.publishedAt DESC")
    List<Video> findPublishedVideos();

    @Query("SELECT v FROM Video v WHERE v.publicationState = 'PUBLISHED' AND v.processingState = 'READY' AND v.blocked = false AND v.deleted = false AND v.visibility IN ('PUBLIC', 'UNLISTED') ORDER BY v.publishedAt DESC")
    List<Video> findPublishedAndUnlistedVideos();

    @Query("SELECT v FROM Video v WHERE v.categoryId = :categoryId AND v.publicationState = 'PUBLISHED' AND v.processingState = 'READY' AND v.blocked = false AND v.deleted = false AND v.visibility = 'PUBLIC' ORDER BY v.publishedAt DESC")
    List<Video> findByCategory(@Param("categoryId") UUID categoryId);

    @Query("SELECT v FROM Video v WHERE v.genreId = :genreId AND v.publicationState = 'PUBLISHED' AND v.processingState = 'READY' AND v.blocked = false AND v.deleted = false AND v.visibility = 'PUBLIC' ORDER BY v.publishedAt DESC")
    List<Video> findByGenre(@Param("genreId") UUID genreId);

    @Query("SELECT v FROM Video v WHERE v.processingState = :state AND v.deleted = false")
    List<Video> findByProcessingState(@Param("state") Video.ProcessingState state);

    @Query("SELECT v FROM Video v WHERE v.deleted = true")
    List<Video> findDeletedVideos();

    @Query("SELECT v FROM Video v WHERE v.creatorId = :creatorId AND v.deleted = true")
    List<Video> findDeletedVideosByCreatorId(@Param("creatorId") UUID creatorId);

    @Query("SELECT v FROM Video v WHERE v.transcodingJobId = :jobId")
    Optional<Video> findByTranscodingJobId(@Param("jobId") UUID jobId);

    @Query("SELECT v FROM Video v WHERE v.channelId IN :channelIds AND v.publicationState = :publicationState AND v.processingState = :processingState AND v.blocked = false AND v.deleted = false ORDER BY v.publishedAt DESC")
    Page<Video> findByChannelIdInAndPublicationStateAndProcessingStateAndBlockedFalseAndDeletedFalse(
            @Param("channelIds") List<UUID> channelIds,
            @Param("publicationState") Video.PublicationState publicationState,
            @Param("processingState") Video.ProcessingState processingState,
            Pageable pageable);

    Page<Video> findByCreatorIdAndDeletedFalse(UUID creatorId, Pageable pageable);

    Page<Video> findByChannelIdAndDeletedFalse(UUID channelId, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.publicationState = 'SCHEDULED' AND v.publishedAt IS NOT NULL AND v.publishedAt <= :now AND v.deleted = false AND v.blocked = false AND v.processingState = 'READY'")
    List<Video> findScheduledVideosDue(@Param("now") Instant now);


    @Query("SELECT new com.platform.video.projection.VideoListProjection(v.id, v.title, v.slug, v.thumbnailUrl, v.durationSeconds, COALESCE(v.viewCount, 0), COALESCE(v.likeCount, 0), COALESCE(v.commentCount, 0), v.publishedAt, " +
       "CASE WHEN v.visibility IN ('PUBLIC', 'UNLISTED', 'PRIVATE') THEN v.visibility ELSE 'PRIVATE' END, " +
       "CASE WHEN v.processingState IN ('UPLOADING', 'UPLOADED', 'PROCESSING', 'READY', 'PROCESSING_FAILED') THEN v.processingState ELSE 'UPLOADING' END, " +
       "CASE WHEN v.publicationState IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'SCHEDULED') THEN v.publicationState ELSE 'DRAFT' END, " +
       "v.categoryId, v.genreId, v.languageId, '', '', '') " +
       "FROM Video v WHERE v.deleted = false AND v.blocked = false AND v.visibility = 'PUBLIC' AND v.publicationState = 'PUBLISHED' AND v.processingState = 'READY' ORDER BY v.publishedAt DESC")
    Page<VideoListProjection> findAllProjected(Pageable pageable);

    @Query("SELECT new com.platform.video.projection.VideoListProjection(v.id, v.title, v.slug, v.thumbnailUrl, v.durationSeconds, COALESCE(v.viewCount, 0), COALESCE(v.likeCount, 0), COALESCE(v.commentCount, 0), v.publishedAt, " +
       "CASE WHEN v.visibility IN ('PUBLIC', 'UNLISTED', 'PRIVATE') THEN v.visibility ELSE 'PRIVATE' END, " +
       "CASE WHEN v.processingState IN ('UPLOADING', 'UPLOADED', 'PROCESSING', 'READY', 'PROCESSING_FAILED') THEN v.processingState ELSE 'UPLOADING' END, " +
       "CASE WHEN v.publicationState IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'SCHEDULED') THEN v.publicationState ELSE 'DRAFT' END, " +
       "v.categoryId, v.genreId, v.languageId, '', '', '') " +
       "FROM Video v WHERE v.creatorId = :creatorId AND v.deleted = false ORDER BY v.createdAt DESC")
    Page<VideoListProjection> findByCreatorIdProjected(@Param("creatorId") UUID creatorId, Pageable pageable);

    @Query("SELECT new com.platform.video.projection.VideoListProjection(v.id, v.title, v.slug, v.thumbnailUrl, v.durationSeconds, COALESCE(v.viewCount, 0), COALESCE(v.likeCount, 0), COALESCE(v.commentCount, 0), v.publishedAt, " +
       "CASE WHEN v.visibility IN ('PUBLIC', 'UNLISTED', 'PRIVATE') THEN v.visibility ELSE 'PRIVATE' END, " +
       "CASE WHEN v.processingState IN ('UPLOADING', 'UPLOADED', 'PROCESSING', 'READY', 'PROCESSING_FAILED') THEN v.processingState ELSE 'UPLOADING' END, " +
       "CASE WHEN v.publicationState IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'SCHEDULED') THEN v.publicationState ELSE 'DRAFT' END, " +
       "v.categoryId, v.genreId, v.languageId, '', '', '') " +
       "FROM Video v WHERE v.channelId = :channelId AND v.deleted = false AND v.blocked = false AND v.visibility = 'PUBLIC' AND v.publicationState = 'PUBLISHED' AND v.processingState = 'READY' ORDER BY v.publishedAt DESC")
    Page<VideoListProjection> findByChannelIdProjected(@Param("channelId") UUID channelId, Pageable pageable);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") UUID videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount + 1 WHERE v.id = :videoId")
    void incrementLikeCount(@Param("videoId") UUID videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount - 1 WHERE v.id = :videoId AND v.likeCount > 0")
    void decrementLikeCount(@Param("videoId") UUID videoId);

    @Modifying
    @Query("UPDATE Video v SET v.commentCount = v.commentCount + 1 WHERE v.id = :videoId")
    void incrementCommentCount(@Param("videoId") UUID videoId);

    @Modifying
    @Query("UPDATE Video v SET v.commentCount = v.commentCount - 1 WHERE v.id = :videoId AND v.commentCount > 0")
    void decrementCommentCount(@Param("videoId") UUID videoId);


    @Transactional(readOnly = true)
    @Query("""
    SELECT v FROM Video v
    WHERE (:categories IS NULL OR v.categoryId IN :categories)
      AND (:genres IS NULL OR v.genreId IN :genres)
      AND v.id NOT IN :excludedIds
      AND v.publicationState = 'PUBLISHED'
      AND v.processingState = 'READY'
      AND v.blocked = false
      AND v.deleted = false
      AND v.visibility = 'PUBLIC'
    ORDER BY RAND()
    """)
    List<Video> findRecommendedVideos(
            @Param("categories") Set<UUID> categories,
            @Param("genres") Set<UUID> genres,
            @Param("excludedIds") Set<UUID> excludedIds,
            Pageable pageable
    );




    @Transactional(readOnly = true)
    @Query("""
    SELECT v FROM Video v
    WHERE v.publicationState = 'PUBLISHED'
      AND v.processingState = 'READY'
      AND v.blocked = false
      AND v.deleted = false
      AND v.visibility = 'PUBLIC'
    ORDER BY v.viewCount DESC, v.likeCount DESC
    """)
    List<Video> findPopularVideos(Pageable pageable);


    @Transactional(readOnly = true)
    @Query("SELECT SUM(v.viewCount) FROM Video v WHERE v.creatorId = :creatorId AND v.deleted = false")
    Long getTotalViewsByCreatorId(@Param("creatorId") UUID creatorId);

    @Transactional(readOnly = true)
    @Query("SELECT SUM(v.likeCount) FROM Video v WHERE v.creatorId = :creatorId AND v.deleted = false")
    Long getTotalLikesByCreatorId(@Param("creatorId") UUID creatorId);

    @Transactional(readOnly = true)
    @Query("SELECT SUM(v.commentCount) FROM Video v WHERE v.creatorId = :creatorId AND v.deleted = false")
    Long getTotalCommentsByCreatorId(@Param("creatorId") UUID creatorId);

    @Transactional(readOnly = true)
    @Query("""
    SELECT COUNT(v)
    FROM Video v
    WHERE v.creatorId = :creatorId
      AND v.deleted = false
    """)
    int countNonDeletedVideosByCreatorId(
            @Param("creatorId") UUID creatorId
    );
}
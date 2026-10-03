package com.platform.common.repository;

import com.platform.common.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

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
}
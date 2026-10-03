package com.platform.search.repository;

import com.platform.search.document.VideoSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoSearchRepository extends ElasticsearchRepository<VideoSearchDocument, String> {

    Optional<VideoSearchDocument> findByVideoId(String videoId);

    Page<VideoSearchDocument> findByIsPublishedTrueAndVisibilityInAndDeletedFalse(
            List<String> visibilities, Pageable pageable);

    Page<VideoSearchDocument> findByChannelIdAndIsPublishedTrueAndDeletedFalse(
            String channelId, Pageable pageable);

    Page<VideoSearchDocument> findByCategoryIdAndIsPublishedTrueAndDeletedFalse(
            String categoryId, Pageable pageable);

    Page<VideoSearchDocument> findByGenreIdAndIsPublishedTrueAndDeletedFalse(
            String genreId, Pageable pageable);

    Page<VideoSearchDocument> findByLanguageIdAndIsPublishedTrueAndDeletedFalse(
            String languageId, Pageable pageable);

    Page<VideoSearchDocument> findByCreatorIdAndIsPublishedTrueAndDeletedFalse(
            String creatorId, Pageable pageable);

    List<VideoSearchDocument> findByTitleSuggestStartingWith(String prefix);
}
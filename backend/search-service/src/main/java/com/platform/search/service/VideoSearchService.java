package com.platform.search.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.json.JsonData;

import java.time.Instant;
import com.platform.common.event.VideoDeletedEvent;
import com.platform.common.event.VideoPublishedEvent;
import com.platform.search.document.VideoSearchDocument;
import com.platform.search.dto.SearchRequest;
import com.platform.search.dto.SearchResponse;
import com.platform.search.dto.SuggestResponse;
import com.platform.search.dto.VideoSearchResult;
import com.platform.search.repository.VideoSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoSearchService {

    private final VideoSearchRepository videoSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Index a newly published video.
     */
    public void indexVideo(VideoPublishedEvent event) {

        if (event == null || event.getVideoId() == null) {
            log.warn("Ignoring video indexing request because event/videoId is null");
            return;
        }

        VideoSearchDocument doc = VideoSearchDocument.builder()
                .videoId(event.getVideoId().toString())
                .slug(event.getSlug())
                .title(event.getTitle())
                .description(event.getDescription())

                .creatorId(
                        event.getCreatorId() != null
                                ? event.getCreatorId().toString()
                                : null
                )

                .channelId(
                        event.getChannelId() != null
                                ? event.getChannelId().toString()
                                : null
                )

                .channelName(event.getChannelName())

                .tags(parseTags(event.getTags()))

                .genreId(
                        event.getGenreId() != null
                                ? event.getGenreId().toString()
                                : null
                )

                .genreName(event.getGenreName())

                .categoryId(
                        event.getCategoryId() != null
                                ? event.getCategoryId().toString()
                                : null
                )

                .categoryName(event.getCategoryName())

                .languageId(
                        event.getLanguageId() != null
                                ? event.getLanguageId().toString()
                                : null
                )

                .languageName(event.getLanguageName())

                .durationSeconds(event.getDurationSeconds())

                .viewCount(0L)
                .likeCount(0)
                .commentCount(0)

                .publishedAt(event.getPublishedAt())
                .thumbnailUrl(event.getThumbnailUrl())
                .visibility(event.getVisibility())

                .processingState("READY")
                .isPublished(true)
                .deleted(false)

                .createdAt(
                        event.getPublishedAt() != null
                                ? event.getPublishedAt()
                                : Instant.now()
                )

                .updatedAt(Instant.now())
                .build();

        videoSearchRepository.save(doc);

        log.info(
                "Indexed published video into Elasticsearch: {}",
                event.getVideoId()
        );
    }

    /**
     * Update an existing indexed video.
     */
    public void updateVideoIndex(
            String videoId,
            Consumer<VideoSearchDocument> updater
    ) {

        if (videoId == null || videoId.isBlank() || updater == null) {
            return;
        }

        videoSearchRepository.findByVideoId(videoId).ifPresent(doc -> {

            updater.accept(doc);

            doc.setUpdatedAt(Instant.now());

            videoSearchRepository.save(doc);

            log.debug("Updated video index: {}", videoId);
        });
    }

    /**
     * Completely remove a video from the search index.
     */
    public void unindexVideo(String videoId) {

        if (videoId == null || videoId.isBlank()) {
            return;
        }

        videoSearchRepository.findByVideoId(videoId).ifPresent(doc -> {

            videoSearchRepository.delete(doc);

            log.info("Unindexed video: {}", videoId);
        });
    }

    /**
     * Delete a video from the search index.
     */
    public void deleteVideoIndex(String videoId) {

        if (videoId == null || videoId.isBlank()) {
            return;
        }

        videoSearchRepository.findByVideoId(videoId).ifPresent(doc -> {

            videoSearchRepository.delete(doc);

            log.info("Deleted video from search index: {}", videoId);
        });
    }

    /**
     * Search videos using Elasticsearch.
     */
    public SearchResponse search(SearchRequest request) {

        long startTime = System.currentTimeMillis();

        if (request == null) {
            request = SearchRequest.builder()
                    .size(20)
                    .sort("relevance")
                    .build();
        }

        Query searchQuery = buildSearchQuery(request);

        /*
         * Cursor is intentionally represented as a Base64 encoded page number.
         *
         * Example:
         * page 0 -> "MA"
         * page 1 -> "MQ"
         * page 2 -> "Mg"
         *
         * Elasticsearch still uses normal from/size pagination underneath.
         */
        int pageNumber = decodeCursor(request.getCursor());

        int requestedSize = request.getSize();

        if (requestedSize <= 0) {
            requestedSize = 20;
        }

        if (requestedSize > 100) {
            requestedSize = 100;
        }

        /*
         * Request one additional document so we can determine whether
         * another page exists.
         */
        Pageable pageable = PageRequest.of(
                pageNumber,
                requestedSize + 1
        );

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(searchQuery)
                .withPageable(pageable);

        applySort(queryBuilder, request);

        NativeQuery query = queryBuilder.build();

        SearchHits<VideoSearchDocument> hits =
                elasticsearchOperations.search(
                        query,
                        VideoSearchDocument.class
                );

        List<VideoSearchResult> results = hits.stream()
                .limit(requestedSize)
                .map(this::mapToResult)
                .toList();

        /*
         * Because we requested size + 1:
         *
         * totalHits > requestedSize
         *
         * means Elasticsearch found enough records to indicate that
         * another page exists.
         */
        boolean hasNext =
                hits.getTotalHits() > ((long) pageNumber * requestedSize + requestedSize);

        /*
         * A safer check for the current returned page:
         * if Elasticsearch actually returned more than requestedSize,
         * another page definitely exists.
         */
        if (hits.getSearchHits().size() > requestedSize) {
            hasNext = true;
        }

        String nextCursor =
                hasNext
                        ? encodeCursor(pageNumber + 1)
                        : null;

        return SearchResponse.builder()
                .results(results)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .totalHits(hits.getTotalHits())
                .tookMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Build the complete Elasticsearch query.
     */
    private Query buildSearchQuery(SearchRequest request) {

        return Query.of(q -> q.bool(bool -> {

            /*
             * ---------------------------------------------------------
             * FULL TEXT SEARCH
             * ---------------------------------------------------------
             */
            if (request.getQ() != null
                    && !request.getQ().isBlank()) {

                bool.must(
                        Query.of(m -> m.multiMatch(mm -> mm
                                .query(request.getQ())
                                .fields(
                                        "title^3",
                                        "description",
                                        "channelName^2",
                                        "tags"
                                )
                                .type(TextQueryType.BestFields)
                                .fuzziness("AUTO")
                        ))
                );

            } else {

                bool.must(
                        Query.of(m -> m.matchAll(
                                ma -> ma
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * PUBLISHED FILTER
             * ---------------------------------------------------------
             */
            if (Boolean.TRUE.equals(request.getIsPublished())) {

                bool.filter(
                        Query.of(t -> t.term(term -> term
                                .field("isPublished")
                                .value(true)
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * DELETED FILTER
             * ---------------------------------------------------------
             *
             * Deleted videos must never appear in search results.
             */
            bool.filter(
                    Query.of(t -> t.term(term -> term
                            .field("deleted")
                            .value(false)
                    ))
            );

            /*
             * ---------------------------------------------------------
             * VISIBILITY FILTER
             * ---------------------------------------------------------
             *
             * If the caller supplies visibility values, create an OR
             * condition:
             *
             *     PUBLIC OR UNLISTED
             *
             * We intentionally avoid FieldValue/terms() here because
             * the Elasticsearch client version used by this project
             * does not resolve FieldValue in the current source setup.
             */
            if (request.getVisibility() != null
                    && !request.getVisibility().isEmpty()) {

                bool.filter(
                        Query.of(visibilityQuery -> visibilityQuery.bool(visibilityBool -> {

                            visibilityBool.minimumShouldMatch("1");

                            request.getVisibility().forEach(visibility -> {

                                if (visibility != null
                                        && !visibility.isBlank()) {

                                    visibilityBool.should(
                                            Query.of(termQuery -> termQuery.term(term -> term
                                                    .field("visibility")
                                                    .value(visibility)
                                            ))
                                    );
                                }
                            });

                            return visibilityBool;
                        }))
                );

            } else {

                /*
                 * Default public search visibility:
                 *
                 *     PUBLIC OR UNLISTED
                 */
                bool.filter(
                        Query.of(visibilityQuery -> visibilityQuery.bool(visibilityBool -> {

                            visibilityBool.minimumShouldMatch("1");

                            visibilityBool.should(
                                    Query.of(publicQuery -> publicQuery.term(term -> term
                                            .field("visibility")
                                            .value("PUBLIC")
                                    ))
                            );

                            visibilityBool.should(
                                    Query.of(unlistedQuery -> unlistedQuery.term(term -> term
                                            .field("visibility")
                                            .value("UNLISTED")
                                    ))
                            );

                            return visibilityBool;
                        }))
                );
            }

            /*
             * ---------------------------------------------------------
             * PROCESSING STATE
             * ---------------------------------------------------------
             */
            if (request.getProcessingState() != null
                    && !request.getProcessingState().isBlank()) {

                bool.filter(
                        Query.of(t -> t.term(term -> term
                                .field("processingState")
                                .value(request.getProcessingState())
                        ))
                );

            } else {

                /*
                 * Only READY videos are searchable by default.
                 */
                bool.filter(
                        Query.of(t -> t.term(term -> term
                                .field("processingState")
                                .value("READY")
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * GENRE
             * ---------------------------------------------------------
             */
            if (request.getGenreId() != null) {

                bool.filter(
                        Query.of(t -> t.term(term -> term
                                .field("genreId")
                                .value(request.getGenreId().toString())
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * CATEGORY
             * ---------------------------------------------------------
             */
            if (request.getCategoryId() != null) {

                bool.filter(
                        Query.of(t -> t.term(term -> term
                                .field("categoryId")
                                .value(request.getCategoryId().toString())
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * CREATOR
             * ---------------------------------------------------------
             */
            if (request.getCreatorId() != null) {

                bool.filter(
                        Query.of(t -> t.term(term -> term
                                .field("creatorId")
                                .value(request.getCreatorId().toString())
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * CHANNEL
             * ---------------------------------------------------------
             */
            if (request.getChannelId() != null) {

                bool.filter(
                        Query.of(t -> t.term(term -> term
                                .field("channelId")
                                .value(request.getChannelId().toString())
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * LANGUAGE
             * ---------------------------------------------------------
             */
            if (request.getLanguageId() != null) {

                bool.filter(
                        Query.of(t -> t.term(term -> term
                                .field("languageId")
                                .value(request.getLanguageId().toString())
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * DURATION RANGE
             * ---------------------------------------------------------
             */
            if (request.getDurationMin() != null) {

                bool.filter(
                        Query.of(r -> r.range(range -> range
                                .field("durationSeconds")
                                .gte(
                                        JsonData.of(
                                                request.getDurationMin()
                                        )
                                )
                        ))
                );
            }

            if (request.getDurationMax() != null) {

                bool.filter(
                        Query.of(r -> r.range(range -> range
                                .field("durationSeconds")
                                .lte(
                                        JsonData.of(
                                                request.getDurationMax()
                                        )
                                )
                        ))
                );
            }

            /*
             * ---------------------------------------------------------
             * PUBLISHED AFTER
             * ---------------------------------------------------------
             */
            if (request.getPublishedAfter() != null
                    && !request.getPublishedAfter().isBlank()) {

                Instant after = parseInstant(
                        request.getPublishedAfter()
                );

                if (after != null) {

                    bool.filter(
                            Query.of(r -> r.range(range -> range
                                    .field("publishedAt")
                                    .gte(
                                            JsonData.of(
                                                    after.toString()
                                            )
                                    )
                            ))
                    );
                }
            }

            /*
             * ---------------------------------------------------------
             * PUBLISHED BEFORE
             * ---------------------------------------------------------
             */
            if (request.getPublishedBefore() != null
                    && !request.getPublishedBefore().isBlank()) {

                Instant before = parseInstant(
                        request.getPublishedBefore()
                );

                if (before != null) {

                    bool.filter(
                            Query.of(r -> r.range(range -> range
                                    .field("publishedAt")
                                    .lte(
                                            JsonData.of(
                                                    before.toString()
                                            )
                                    )
                            ))
                    );
                }
            }

            return bool;
        }));
    }

    /**
     * Apply Elasticsearch sorting.
     */
    private void applySort(
            NativeQueryBuilder queryBuilder,
            SearchRequest request
    ) {

        String sort = request.getSort();

        /*
         * Null/blank sort behaves as relevance.
         */
        if (sort == null || sort.isBlank()) {
            sort = "relevance";
        }

        switch (sort) {

            case "publishedAt" -> {

                queryBuilder.withSort(
                        SortOptions.of(s -> s.field(f -> f
                                .field("publishedAt")
                                .order(SortOrder.Desc)
                        ))
                );
            }

            case "viewCount" -> {

                queryBuilder.withSort(
                        SortOptions.of(s -> s.field(f -> f
                                .field("viewCount")
                                .order(SortOrder.Desc)
                        ))
                );
            }

            case "likeCount" -> {

                queryBuilder.withSort(
                        SortOptions.of(s -> s.field(f -> f
                                .field("likeCount")
                                .order(SortOrder.Desc)
                        ))
                );
            }

            default -> {

                /*
                 * If the user supplied a search term, Elasticsearch
                 * relevance score is the default sort.
                 */
                if (request.getQ() != null
                        && !request.getQ().isBlank()) {

                    queryBuilder.withSort(
                            SortOptions.of(s -> s.score(
                                    score -> score.order(
                                            SortOrder.Desc
                                    )
                            ))
                    );

                } else {

                    /*
                     * No search term -> newest videos first.
                     */
                    queryBuilder.withSort(
                            SortOptions.of(s -> s.field(f -> f
                                    .field("publishedAt")
                                    .order(SortOrder.Desc)
                            ))
                    );
                }
            }
        }
    }

    /**
     * Autocomplete / suggestion search.
     */
    public SuggestResponse suggest(String prefix) {

        if (prefix == null || prefix.isBlank()) {

            return SuggestResponse.builder()
                    .suggestions(List.of())
                    .channels(List.of())
                    .tags(List.of())
                    .build();
        }

        /*
         * ---------------------------------------------------------
         * TITLE SUGGESTIONS
         * ---------------------------------------------------------
         */
        NativeQuery titleQuery = NativeQuery.builder()
                .withQuery(
                        Query.of(q -> q.matchBoolPrefix(
                                match -> match
                                        .field("title.edge_ngram")
                                        .query(prefix)
                        ))
                )
                .withPageable(
                        PageRequest.of(0, 10)
                )
                .build();

        SearchHits<VideoSearchDocument> titleHits =
                elasticsearchOperations.search(
                        titleQuery,
                        VideoSearchDocument.class
                );

        List<String> titleSuggestions = titleHits.stream()
                .map(SearchHit::getContent)
                .map(VideoSearchDocument::getTitle)
                .filter(title ->
                        title != null
                                && !title.isBlank()
                )
                .distinct()
                .limit(5)
                .toList();

        /*
         * ---------------------------------------------------------
         * CHANNEL SUGGESTIONS
         * ---------------------------------------------------------
         */
        NativeQuery channelQuery = NativeQuery.builder()
                .withQuery(
                        Query.of(q -> q.matchBoolPrefix(
                                match -> match
                                        .field("channelName.edge_ngram")
                                        .query(prefix)
                        ))
                )
                .withPageable(
                        PageRequest.of(0, 10)
                )
                .build();

        SearchHits<VideoSearchDocument> channelHits =
                elasticsearchOperations.search(
                        channelQuery,
                        VideoSearchDocument.class
                );

        List<SuggestResponse.ChannelSuggestion> channelSuggestions =
                channelHits.stream()
                        .map(SearchHit::getContent)
                        .filter(doc ->
                                doc.getChannelId() != null
                                        && doc.getChannelName() != null
                        )
                        .map(doc ->
                                SuggestResponse.ChannelSuggestion.builder()
                                        .channelId(doc.getChannelId())
                                        .channelName(doc.getChannelName())

                                        /*
                                         * VideoSearchDocument currently
                                         * does not contain channelSlug.
                                         *
                                         * Keep the existing contract by
                                         * using channelId here.
                                         */
                                        .channelSlug(doc.getChannelId())

                                        .avatarUrl(null)
                                        .subscriberCount(0L)
                                        .build()
                        )
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        suggestion ->
                                                suggestion.getChannelId()
                                                        + "|"
                                                        + suggestion.getChannelName(),
                                        suggestion -> suggestion,
                                        (first, second) -> first
                                ),
                                map -> map.values()
                                        .stream()
                                        .limit(5)
                                        .toList()
                        ));

        /*
         * ---------------------------------------------------------
         * TAG SUGGESTIONS
         * ---------------------------------------------------------
         *
         * The current VideoSearchDocument does not expose a dedicated
         * tag aggregation endpoint here. Keep the existing API contract
         * and return an empty list until a terms aggregation is added.
         */
        List<SuggestResponse.TagSuggestion> tagSuggestions =
                List.of();

        return SuggestResponse.builder()
                .suggestions(titleSuggestions)
                .channels(channelSuggestions)
                .tags(tagSuggestions)
                .build();
    }

    /**
     * Convert Elasticsearch SearchHit to API response.
     */
    private VideoSearchResult mapToResult(
            SearchHit<VideoSearchDocument> hit
    ) {

        VideoSearchDocument doc = hit.getContent();

        return VideoSearchResult.builder()
                .videoId(doc.getVideoId())
                .slug(doc.getSlug())
                .title(doc.getTitle())
                .description(doc.getDescription())

                .creatorId(doc.getCreatorId())

                .channelId(doc.getChannelId())
                .channelName(doc.getChannelName())

                .tags(doc.getTags())

                .genreId(doc.getGenreId())
                .genreName(doc.getGenreName())

                .categoryId(doc.getCategoryId())
                .categoryName(doc.getCategoryName())

                .languageId(doc.getLanguageId())
                .languageName(doc.getLanguageName())

                .durationSeconds(doc.getDurationSeconds())

                .viewCount(doc.getViewCount())
                .likeCount(doc.getLikeCount())
                .commentCount(doc.getCommentCount())

                .publishedAt(doc.getPublishedAt())

                .thumbnailUrl(doc.getThumbnailUrl())

                .visibility(doc.getVisibility())
                .processingState(doc.getProcessingState())

                .isPublished(doc.getIsPublished())

                .score(hit.getScore())

                .build();
    }

    /**
     * Decode cursor into a page number.
     */
    private int decodeCursor(String cursor) {

        if (cursor == null || cursor.isBlank()) {
            return 0;
        }

        try {

            String decoded =
                    new String(
                            Base64.getUrlDecoder().decode(cursor),
                            StandardCharsets.UTF_8
                    );

            return Math.max(
                    0,
                    Integer.parseInt(decoded)
            );

        } catch (Exception ignored) {

            log.debug(
                    "Invalid search cursor received: {}",
                    cursor
            );

            return 0;
        }
    }

    /**
     * Encode page number as URL-safe Base64 cursor.
     */
    private String encodeCursor(int page) {

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        String.valueOf(page)
                                .getBytes(StandardCharsets.UTF_8)
                );
    }

    /**
     * Convert comma-separated tags to a Set.
     */
    private Set<String> parseTags(String tags) {

        if (tags == null || tags.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Safely parse an ISO-8601 Instant.
     */
    private Instant parseInstant(String value) {

        try {

            return Instant.parse(value);

        } catch (Exception exception) {

            log.warn(
                    "Ignoring invalid publishedAt date '{}'",
                    value
            );

            return null;
        }
    }
}
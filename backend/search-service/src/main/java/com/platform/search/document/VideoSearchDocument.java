package com.platform.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enhanced Video Search Document for Feature 10: Advanced Elasticsearch Search
 * Adds support for transcript search, geographic search, and enhanced analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "videos_v1")
@Setting(settingPath = "elasticsearch/index-settings.json")
@Mapping(mappingPath = "elasticsearch/index-mapping.json")
public class VideoSearchDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private String videoId;

    @Field(type = FieldType.Keyword)
    private String slug;

    /*
     * ---------------------------------------------------------
     * TITLE
     * ---------------------------------------------------------
     *
     * Main field:
     *   title
     *
     * Additional fields:
     *   title.keyword
     *   title.edge_ngram
     *
     * Completion suggestions are handled separately by
     * @CompletionField rather than FieldType.Completion.
     */
    @MultiField(
            mainField = @Field(
                    type = FieldType.Text,
                    analyzer = "video_analyzer",
                    searchAnalyzer = "video_search_analyzer"
            ),
            otherFields = {
                    @InnerField(
                            suffix = "keyword",
                            type = FieldType.Keyword
                    ),
                    @InnerField(
                            suffix = "edge_ngram",
                            type = FieldType.Text,
                            analyzer = "edge_ngram_analyzer",
                            searchAnalyzer = "keyword_analyzer"
                    )
            }
    )
    private String title;

    @Field(
            type = FieldType.Text,
            analyzer = "video_analyzer",
            searchAnalyzer = "video_search_analyzer"
    )
    private String description;

    /** Transcript/searchable text from video audio (for searching within video content) */
    @Field(
            type = FieldType.Text,
            analyzer = "video_analyzer",
            searchAnalyzer = "video_search_analyzer"
    )
    private String transcript;

    @Field(type = FieldType.Keyword)
    private String creatorId;

    @Field(type = FieldType.Keyword)
    private String channelId;

    /*
     * ---------------------------------------------------------
     * CHANNEL NAME
     * ---------------------------------------------------------
     *
     * Generates:
     *
     * channelName
     * channelName.keyword
     * channelName.edge_ngram
     */
    @MultiField(
            mainField = @Field(
                    type = FieldType.Text,
                    analyzer = "video_analyzer"
            ),
            otherFields = {
                    @InnerField(
                            suffix = "keyword",
                            type = FieldType.Keyword
                    ),
                    @InnerField(
                            suffix = "edge_ngram",
                            type = FieldType.Text,
                            analyzer = "edge_ngram_analyzer",
                            searchAnalyzer = "keyword_analyzer"
                    )
            }
    )
    private String channelName;

    @Field(type = FieldType.Keyword)
    private Set<String> tags;

    @Field(type = FieldType.Keyword)
    private String genreId;

    @Field(type = FieldType.Keyword)
    private String genreName;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String languageId;

    @Field(type = FieldType.Keyword)
    private String languageName;

    @Field(type = FieldType.Integer)
    private Integer durationSeconds;

    @Field(type = FieldType.Long)
    private Long viewCount;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Integer)
    private Integer commentCount;

    /** Geographic data for location-based search */
    // @Field(type = FieldType.GeoPoint)
    private List<Double> location; // [lat, lon]

    @Field(type = FieldType.Keyword)
    private String countryCode;

    @Field(type = FieldType.Keyword)
    private String regionCode;

    @Field(type = FieldType.Keyword)
    private String cityName;

    @Field(
            type = FieldType.Date,
            format = DateFormat.date_optional_time
    )
    private Instant publishedAt;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    @Field(type = FieldType.Keyword)
    private String visibility;

    @Field(type = FieldType.Keyword)
    private String processingState;

    @Field(type = FieldType.Boolean)
    private Boolean isPublished;

    @Field(type = FieldType.Boolean)
    private Boolean deleted;

    /** Enhanced engagement metrics for better ranking */
    @Field(type = FieldType.Double)
    private Double engagementScore; // Calculated engagement score for ranking

    @Field(type = FieldType.Integer)
    private Integer watchTimeSeconds; // Total watch time

    @Field(type = FieldType.Integer)
    private Integer shares; // Share count

    @Field(type = FieldType.Integer)
    private Integer subscribersGained; // Subscribers gained from this video

    @Field(
            type = FieldType.Date,
            format = DateFormat.date_optional_time
    )
    private Instant createdAt;

    @Field(
            type = FieldType.Date,
            format = DateFormat.date_optional_time
    )
    private Instant updatedAt;
}
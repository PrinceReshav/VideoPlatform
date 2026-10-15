package com.platform.user.mapper;

import com.platform.user.dto.CategoryResponse;
import com.platform.user.dto.ChannelResponse;
import com.platform.user.dto.CreatorProfileResponse;
import com.platform.user.dto.GenreResponse;
import com.platform.user.dto.LanguageResponse;
import com.platform.user.entity.Category;
import com.platform.user.entity.Channel;
import com.platform.user.entity.CreatorProfile;
import com.platform.user.entity.Genre;
import com.platform.user.entity.Language;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Named("toCategoryResponse")
    CategoryResponse toCategoryResponse(Category category);

    @Named("toCategoryResponseWithChildren")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "parentId", source = "parent.id")
    CategoryResponse toCategoryResponseWithChildren(Category category);

    @Named("toCategoryResponseList")
    List<CategoryResponse> toCategoryResponseList(List<Category> categories);

    @Named("toGenreResponse")
    GenreResponse toGenreResponse(Genre genre);

    @Named("toGenreResponseWithChildren")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "parentId", source = "parent.id")
    GenreResponse toGenreResponseWithChildren(Genre genre);

    @Named("toGenreResponseList")
    List<GenreResponse> toGenreResponseList(List<Genre> genres);

    @Named("toLanguageResponse")
    LanguageResponse toLanguageResponse(Language language);

    @Named("toLanguageResponseList")
    List<LanguageResponse> toLanguageResponseList(List<Language> languages);

    @Named("toCreatorProfileResponseWithDetails")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(
            target = "genreIds",
            expression = "java(mapGenreIds(profile.getGenres()))"
    )
    @Mapping(
            target = "genreNames",
            expression = "java(mapGenreNames(profile.getGenres()))"
    )
    @Mapping(target = "defaultLanguageId", source = "defaultLanguageId")
    @Mapping(target = "defaultLanguageName", ignore = true)
    @Mapping(target = "customUrl", ignore = true)
    @Mapping(target = "keywords", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "email", ignore = true)
    CreatorProfileResponse toCreatorProfileResponseWithDetails(
            CreatorProfile profile
    );

    @Mapping(target = "listed", source = "listed")
    @Mapping(target = "allowsCommunity", source = "allowsCommunity")
    @Mapping(target = "allowsMessaging", source = "allowsMessaging")
    @Mapping(target = "defaultVideoPrivacy", source = "defaultVideoPrivacy")
    @Mapping(target = "defaultLicense", source = "defaultLicense")
    @Mapping(target = "showSubscriberCount", source = "showSubscriberCount")
    @Mapping(target = "showRatingCounts", source = "showRatingCounts")
    @Mapping(target = "verificationTier", source = "verificationTier")
    @Mapping(target = "channelKeywords", source = "channelKeywords")
    @Mapping(target = "adsenseAccountId", source = "adsenseAccountId")
    @Mapping(target = "googleAnalyticsId", source = "googleAnalyticsId")
    @Mapping(target = "trailerVideoId", source = "trailerVideoId")
    @Mapping(target = "featuredChannels", source = "featuredChannels")
    @Mapping(target = "featuredPlaylists", source = "featuredPlaylists")
    @Mapping(target = "countryRestriction", source = "countryRestriction")
    @Mapping(target = "ageRestriction", source = "ageRestriction")
    @Mapping(target = "madeForKids", source = "madeForKids")
    @Mapping(target = "autoChaptersEnabled", source = "autoChaptersEnabled")
    @Mapping(target = "autoCaptionsEnabled", source = "autoCaptionsEnabled")
    @Mapping(target = "defaultCommentSetting", source = "defaultCommentSetting")
    ChannelResponse toChannelResponse(Channel channel);

    @Named("toChannelResponseWithDetails")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(
            target = "genreIds",
            expression = "java(mapGenreIds(channel.getGenres()))"
    )
    @Mapping(
            target = "genreNames",
            expression = "java(mapGenreNames(channel.getGenres()))"
    )
    @Mapping(target = "defaultLanguageId", source = "defaultLanguageId")
    @Mapping(target = "defaultLanguageName", ignore = true)
    @Mapping(target = "customUrl", ignore = true)
    @Mapping(target = "keywords", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "listed", source = "listed")
    @Mapping(target = "allowsCommunity", source = "allowsCommunity")
    @Mapping(target = "allowsMessaging", source = "allowsMessaging")
    @Mapping(target = "defaultVideoPrivacy", source = "defaultVideoPrivacy")
    @Mapping(target = "defaultLicense", source = "defaultLicense")
    @Mapping(target = "showSubscriberCount", source = "showSubscriberCount")
    @Mapping(target = "showRatingCounts", source = "showRatingCounts")
    @Mapping(target = "verificationTier", source = "verificationTier")
    @Mapping(target = "channelKeywords", source = "channelKeywords")
    @Mapping(target = "adsenseAccountId", source = "adsenseAccountId")
    @Mapping(target = "googleAnalyticsId", source = "googleAnalyticsId")
    @Mapping(target = "trailerVideoId", source = "trailerVideoId")
    @Mapping(target = "featuredChannels", source = "featuredChannels")
    @Mapping(target = "featuredPlaylists", source = "featuredPlaylists")
    @Mapping(target = "countryRestriction", source = "countryRestriction")
    @Mapping(target = "ageRestriction", source = "ageRestriction")
    @Mapping(target = "madeForKids", source = "madeForKids")
    @Mapping(target = "autoChaptersEnabled", source = "autoChaptersEnabled")
    @Mapping(target = "autoCaptionsEnabled", source = "autoCaptionsEnabled")
    @Mapping(target = "defaultCommentSetting", source = "defaultCommentSetting")
    ChannelResponse toChannelResponseWithDetails(Channel channel);

    @Named("mapGenreIds")
    default Set<UUID> mapGenreIds(Set<Genre> genres) {
        if (genres == null) {
            return Set.of();
        }

        return genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
    }

    @Named("mapGenreNames")
    default Set<String> mapGenreNames(Set<Genre> genres) {
        if (genres == null) {
            return Set.of();
        }

        return genres.stream()
                .map(Genre::getName)
                .collect(Collectors.toSet());
    }

    @Named("getLanguageName")
    default String getLanguageName(UUID languageId) {
        // Language lookup requires a service-level implementation.
        return null;
    }
}
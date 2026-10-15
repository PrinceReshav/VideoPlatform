package com.platform.user.service;

import com.platform.user.dto.*;
import com.platform.user.entity.*;
import com.platform.user.repository.*;
import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.util.SlugUtil;
import com.platform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final LanguageRepository languageRepository;
    private final UserMapper userMapper;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional(readOnly = true)
    public ChannelResponse getChannelById(UUID id) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Channel not found"));
        return enrichChannelResponse(channel);
    }

    @Transactional(readOnly = true)
    public ChannelResponse getChannelBySlug(String slug) {
        Channel channel = channelRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Channel not found"));
        return enrichChannelResponse(channel);
    }

    @Transactional(readOnly = true)
    public ChannelResponse getChannelByCustomUrl(String customUrl) {
        Channel channel = channelRepository.findByCustomUrl(customUrl)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Channel not found"));
        return enrichChannelResponse(channel);
    }

    @Transactional(readOnly = true)
    public ChannelResponse getMyChannel() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        CreatorProfile profile = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "Creator profile not found"));
        
        Channel channel = channelRepository.findByCreatorProfileId(profile.getId())
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CHANNEL_NOT_FOUND, "Channel not found"));
        return enrichChannelResponse(channel);
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> getActiveChannels() {
        return channelRepository.findActiveChannels().stream()
                .map(this::enrichChannelResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ChannelResponse> getChannels(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Channel> channelPage = channelRepository.findByActiveTrueAndDeletedFalseAndSuspendedFalse(pageable);
        return PageResponse.of(
                channelPage.getContent().stream().map(this::enrichChannelResponse).toList(),
                page, size, channelPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> getChannelsByCategory(UUID categoryId) {
        return channelRepository.findByCategory(categoryId).stream()
                .map(this::enrichChannelResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> getVerifiedChannels() {
        return channelRepository.findVerifiedChannels().stream()
                .map(this::enrichChannelResponse)
                .toList();
    }


    @Transactional
    public ChannelResponse createChannel(CreateChannelRequest request) {

        // Verify creator profile exists
        CreatorProfile creatorProfile = creatorProfileRepository
                .findById(request.getCreatorProfileId())
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.RESOURCE_NOT_FOUND,
                        "Creator profile not found"
                ));

        // Verify category exists if provided and retain the entity relationship
        Category category = null;

        if (request.getCategoryId() != null) {
            category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(
                            ApiErrorCodes.RESOURCE_NOT_FOUND,
                            "Category not found"
                    ));
        }

        // Verify default language exists if provided
        if (request.getDefaultLanguageId() != null) {
            languageRepository.findById(request.getDefaultLanguageId())
                    .orElseThrow(() -> new NotFoundException(
                            ApiErrorCodes.RESOURCE_NOT_FOUND,
                            "Language not found"
                    ));
        }

        // Create the channel using the entity's existing setters
        Channel channel = new Channel();

        channel.setCreatorProfileId(creatorProfile.getId());
        channel.setName(request.getName());
        channel.setDescription(request.getDescription());
        channel.setCategory(category);
        channel.setDefaultLanguageId(request.getDefaultLanguageId());
        channel.setCustomUrl(request.getCustomUrl());
        channel.setKeywords(request.getKeywords());
        channel.setCountry(request.getCountry());
        channel.setEmail(request.getEmail());

        // Channel visibility and communication settings
        channel.setListed(request.isListed());
        channel.setAllowsCommunity(request.isAllowsCommunity());
        channel.setAllowsMessaging(request.isAllowsMessaging());

        // Default video settings
        channel.setDefaultVideoPrivacy(
                request.getDefaultVideoPrivacy() != null
                        ? request.getDefaultVideoPrivacy()
                        : "PUBLIC"
        );

        channel.setDefaultLicense(
                request.getDefaultLicense() != null
                        ? request.getDefaultLicense()
                        : "STANDARD_YOUTUBE"
        );

        channel.setShowSubscriberCount(request.isShowSubscriberCount());
        channel.setShowRatingCounts(request.isShowRatingCounts());

        // Verification and channel metadata
        channel.setVerificationTier(
                request.getVerificationTier() != null
                        ? request.getVerificationTier()
                        : "NONE"
        );

        channel.setChannelKeywords(request.getChannelKeywords());
        channel.setAdsenseAccountId(request.getAdsenseAccountId());
        channel.setGoogleAnalyticsId(request.getGoogleAnalyticsId());
        channel.setTrailerVideoId(request.getTrailerVideoId());
        channel.setFeaturedChannels(request.getFeaturedChannels());
        channel.setFeaturedPlaylists(request.getFeaturedPlaylists());
        channel.setCountryRestriction(request.getCountryRestriction());

        channel.setAgeRestriction(
                request.getAgeRestriction() != null
                        ? request.getAgeRestriction()
                        : "NONE"
        );

        // Audience and accessibility settings
        channel.setMadeForKids(request.isMadeForKids());
        channel.setAutoChaptersEnabled(request.isAutoChaptersEnabled());
        channel.setAutoCaptionsEnabled(request.isAutoCaptionsEnabled());

        channel.setDefaultCommentSetting(
                request.getDefaultCommentSetting() != null
                        ? request.getDefaultCommentSetting()
                        : "ALLOW_ALL"
        );

        // Generate a unique slug from the channel name
        String slug = SlugUtil.slugify(request.getName());
        slug = SlugUtil.generateUniqueSlug(
                slug,
                candidate -> channelRepository.existsBySlug(candidate)
        );

        channel.setSlug(slug);

        // Save the channel
        Channel savedChannel = channelRepository.save(channel);

        log.info(
                "Created channel: {} ({})",
                savedChannel.getName(),
                savedChannel.getId()
        );

        return enrichChannelResponse(savedChannel);
    }



    @Transactional
    public ChannelResponse updateChannel(UUID id, UpdateChannelRequest request) {

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ApiErrorCodes.RESOURCE_NOT_FOUND,
                        "Channel not found"
                ));

        // Validate and retrieve category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(
                            ApiErrorCodes.RESOURCE_NOT_FOUND,
                            "Category not found"
                    ));

            channel.setCategory(category);
        }

        // Validate default language if provided
        if (request.getDefaultLanguageId() != null) {
            languageRepository.findById(request.getDefaultLanguageId())
                    .orElseThrow(() -> new NotFoundException(
                            ApiErrorCodes.RESOURCE_NOT_FOUND,
                            "Language not found"
                    ));

            channel.setDefaultLanguageId(request.getDefaultLanguageId());
        }

        // Update name and regenerate slug
        if (request.getName() != null) {
            channel.setName(request.getName());

            String slug = SlugUtil.slugify(request.getName());
            slug = SlugUtil.generateUniqueSlug(
                    slug,
                    candidate -> channelRepository.existsBySlugAndIdNot(candidate, id)
            );

            channel.setSlug(slug);
        }

        if (request.getDescription() != null) {
            channel.setDescription(request.getDescription());
        }

        // Update creator profile if provided
        if (request.getCreatorProfileId() != null) {
            CreatorProfile creatorProfile = creatorProfileRepository
                    .findById(request.getCreatorProfileId())
                    .orElseThrow(() -> new NotFoundException(
                            ApiErrorCodes.RESOURCE_NOT_FOUND,
                            "Creator profile not found"
                    ));

            channel.setCreatorProfileId(creatorProfile.getId());
        }

        if (request.getCustomUrl() != null) {
            channel.setCustomUrl(request.getCustomUrl());
        }

        if (request.getKeywords() != null) {
            channel.setKeywords(request.getKeywords());
        }

        if (request.getCountry() != null) {
            channel.setCountry(request.getCountry());
        }

        if (request.getEmail() != null) {
            channel.setEmail(request.getEmail());
        }

        // Nullable Boolean fields: null means leave the current value unchanged
        if (request.getIsListed() != null) {
            channel.setListed(request.getIsListed());
        }

        if (request.getAllowsCommunity() != null) {
            channel.setAllowsCommunity(request.getAllowsCommunity());
        }

        if (request.getAllowsMessaging() != null) {
            channel.setAllowsMessaging(request.getAllowsMessaging());
        }

        if (request.getDefaultVideoPrivacy() != null) {
            channel.setDefaultVideoPrivacy(request.getDefaultVideoPrivacy());
        }

        if (request.getDefaultLicense() != null) {
            channel.setDefaultLicense(request.getDefaultLicense());
        }

        if (request.getShowSubscriberCount() != null) {
            channel.setShowSubscriberCount(request.getShowSubscriberCount());
        }

        if (request.getShowRatingCounts() != null) {
            channel.setShowRatingCounts(request.getShowRatingCounts());
        }

        if (request.getVerificationTier() != null) {
            channel.setVerificationTier(request.getVerificationTier());
        }

        if (request.getChannelKeywords() != null) {
            channel.setChannelKeywords(request.getChannelKeywords());
        }

        if (request.getAdsenseAccountId() != null) {
            channel.setAdsenseAccountId(request.getAdsenseAccountId());
        }

        if (request.getGoogleAnalyticsId() != null) {
            channel.setGoogleAnalyticsId(request.getGoogleAnalyticsId());
        }

        if (request.getTrailerVideoId() != null) {
            channel.setTrailerVideoId(request.getTrailerVideoId());
        }

        if (request.getFeaturedChannels() != null) {
            channel.setFeaturedChannels(request.getFeaturedChannels());
        }

        if (request.getFeaturedPlaylists() != null) {
            channel.setFeaturedPlaylists(request.getFeaturedPlaylists());
        }

        if (request.getCountryRestriction() != null) {
            channel.setCountryRestriction(request.getCountryRestriction());
        }

        if (request.getAgeRestriction() != null) {
            channel.setAgeRestriction(request.getAgeRestriction());
        }

        if (request.getMadeForKids() != null) {
            channel.setMadeForKids(request.getMadeForKids());
        }

        if (request.getAutoChaptersEnabled() != null) {
            channel.setAutoChaptersEnabled(request.getAutoChaptersEnabled());
        }

        if (request.getAutoCaptionsEnabled() != null) {
            channel.setAutoCaptionsEnabled(request.getAutoCaptionsEnabled());
        }

        if (request.getDefaultCommentSetting() != null) {
            channel.setDefaultCommentSetting(request.getDefaultCommentSetting());
        }

        Channel updatedChannel = channelRepository.save(channel);

        log.info(
                "Updated channel: {} ({})",
                updatedChannel.getName(),
                updatedChannel.getId()
        );

        return enrichChannelResponse(updatedChannel);
    }

    private ChannelResponse enrichChannelResponse(Channel channel) {
        ChannelResponse response = userMapper.toChannelResponseWithDetails(channel);

        if (channel.getCategory() != null) {
            response.setCategoryName(channel.getCategory().getName());
        }

        if (channel.getGenres() != null) {
            response.setGenreNames(channel.getGenres().stream()
                    .map(Genre::getName)
                    .collect(Collectors.toSet()));
        }

        if (channel.getDefaultLanguageId() != null) {
            Language language = languageRepository.findById(channel.getDefaultLanguageId()).orElse(null);
            if (language != null) {
                response.setDefaultLanguageName(language.getName());
            }
        }

        return response;
    }
}
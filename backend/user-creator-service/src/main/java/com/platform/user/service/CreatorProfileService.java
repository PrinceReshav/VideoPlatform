package com.platform.user.service;

import com.platform.user.dto.*;
import com.platform.user.entity.*;
import com.platform.user.repository.*;
import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ConflictException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.util.SlugUtil;
import com.platform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorProfileService {

    private final CreatorProfileRepository creatorProfileRepository;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final LanguageRepository languageRepository;
    private final UserMapper userMapper;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional
    public CreatorProfileResponse becomeCreator(CreateCreatorProfileRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        if (creatorProfileRepository.existsByUserId(userId)) {
            throw new ConflictException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "User is already a creator");
        }

        if (request.getChannelName() != null && creatorProfileRepository.existsByChannelName(request.getChannelName())) {
            throw new ConflictException(ApiErrorCodes.CHANNEL_NAME_TAKEN, "Channel name already taken");
        }

        String channelSlug = SlugUtil.slugify(request.getChannelName());
        channelSlug = SlugUtil.generateUniqueSlug(channelSlug, s -> creatorProfileRepository.existsByChannelSlug(s));

        CreatorProfile profile = new CreatorProfile();
        profile.setUserId(userId);
        profile.setChannelName(request.getChannelName());
        profile.setChannelSlug(channelSlug);
        profile.setDescription(request.getDescription());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setLocation(request.getLocation());
        profile.setCustomUrl(request.getCustomUrl());
        profile.setKeywords(request.getKeywords());
        profile.setCountry(request.getCountry());
        profile.setEmail(request.getEmail());
        profile.setActive(true);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));
            profile.setCategory(category);
        }

        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
            profile.setGenres(new java.util.HashSet<>(genres));
        }

        if (request.getDefaultLanguageId() != null) {
            Language language = languageRepository.findById(request.getDefaultLanguageId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));
            profile.setDefaultLanguageId(language.getId());
        }

        profile = creatorProfileRepository.save(profile);

        // Create corresponding channel
        Channel channel = new Channel();
        channel.setCreatorProfileId(profile.getId());
        channel.setName(request.getChannelName());
        channel.setSlug(channelSlug);
        channel.setDescription(request.getDescription());
        channel.setCustomUrl(request.getCustomUrl());
        channel.setKeywords(request.getKeywords());
        channel.setCountry(request.getCountry());
        channel.setEmail(request.getEmail());
        channel.setActive(true);

        if (request.getCategoryId() != null) {
            channel.setCategory(profile.getCategory());
        }
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            channel.setGenres(profile.getGenres());
        }
        if (request.getDefaultLanguageId() != null) {
            channel.setDefaultLanguageId(profile.getDefaultLanguageId());
        }

        channelRepository.save(channel);

        log.info("User {} became creator with channel {}", userId, profile.getChannelName());
        return userMapper.toCreatorProfileResponseWithDetails(profile);
    }

    @Transactional(readOnly = true)
    public CreatorProfileResponse getMyCreatorProfile() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        CreatorProfile profile = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "Creator profile not found"));
        return enrichProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public CreatorProfileResponse getCreatorProfileById(UUID id) {
        CreatorProfile profile = creatorProfileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "Creator profile not found"));
        return enrichProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public CreatorProfileResponse getCreatorProfileByChannelSlug(String channelSlug) {
        CreatorProfile profile = creatorProfileRepository.findByChannelSlug(channelSlug)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "Creator profile not found"));
        return enrichProfileResponse(profile);
    }

    @Transactional
    public CreatorProfileResponse updateCreatorProfile(UpdateCreatorProfileRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        CreatorProfile profile = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "Creator profile not found"));

        if (request.getChannelName() != null && !request.getChannelName().equals(profile.getChannelName())) {
            if (creatorProfileRepository.existsByChannelName(request.getChannelName())) {
                throw new ConflictException(ApiErrorCodes.CHANNEL_NAME_TAKEN, "Channel name already taken");
            }
            String channelSlug = SlugUtil.slugify(request.getChannelName());
            channelSlug = SlugUtil.generateUniqueSlug(channelSlug, s -> creatorProfileRepository.existsByChannelSlug(s));
            profile.setChannelName(request.getChannelName());
            profile.setChannelSlug(channelSlug);
        }

        if (request.getDescription() != null) profile.setDescription(request.getDescription());
        if (request.getWebsiteUrl() != null) profile.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getLocation() != null) profile.setLocation(request.getLocation());
        if (request.getCustomUrl() != null) profile.setCustomUrl(request.getCustomUrl());
        if (request.getKeywords() != null) profile.setKeywords(request.getKeywords());
        if (request.getCountry() != null) profile.setCountry(request.getCountry());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getBannerUrl() != null) profile.setBannerUrl(request.getBannerUrl());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));
            profile.setCategory(category);
        } else if (request.getCategoryId() != null && request.getCategoryId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            profile.setCategory(null);
        }

        if (request.getGenreIds() != null) {
            List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
            profile.setGenres(new java.util.HashSet<>(genres));
        }

        if (request.getDefaultLanguageId() != null) {
            Language language = languageRepository.findById(request.getDefaultLanguageId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));
            profile.setDefaultLanguageId(language.getId());
        } else if (request.getDefaultLanguageId() != null && request.getDefaultLanguageId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            profile.setDefaultLanguageId(null);
        }

        profile = creatorProfileRepository.save(profile);

        // Also update channel
        Channel channel = channelRepository.findByCreatorProfileId(profile.getId())
                .orElse(null);
        if (channel != null) {
            if (request.getChannelName() != null) {
                channel.setName(request.getChannelName());
                channel.setSlug(profile.getChannelSlug());
            }
            if (request.getDescription() != null) channel.setDescription(request.getDescription());
            if (request.getCustomUrl() != null) channel.setCustomUrl(request.getCustomUrl());
            if (request.getKeywords() != null) channel.setKeywords(request.getKeywords());
            if (request.getCountry() != null) channel.setCountry(request.getCountry());
            if (request.getEmail() != null) channel.setEmail(request.getEmail());
            if (request.getCategoryId() != null) {
                channel.setCategory(profile.getCategory());
            }
            if (request.getGenreIds() != null) {
                channel.setGenres(profile.getGenres());
            }
            if (request.getDefaultLanguageId() != null) {
                channel.setDefaultLanguageId(profile.getDefaultLanguageId());
            }
            channelRepository.save(channel);
        }

        log.info("Updated creator profile: {}", profile.getId());
        return enrichProfileResponse(profile);
    }

    @Transactional
    public void deleteCreatorProfile() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        CreatorProfile profile = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "Creator profile not found"));

        profile.markDeleted(userId);
        creatorProfileRepository.save(profile);

        // Soft delete channel
        Channel channel = channelRepository.findByCreatorProfileId(profile.getId())
                .orElse(null);
        if (channel != null) {
            channel.markDeleted(userId);
            channelRepository.save(channel);
        }

        log.info("Deleted creator profile for user: {}", userId);
    }

    private CreatorProfileResponse enrichProfileResponse(CreatorProfile profile) {
        CreatorProfileResponse response = userMapper.toCreatorProfileResponseWithDetails(profile);

        // Resolve category name
        if (profile.getCategory() != null) {
            response.setCategoryName(profile.getCategory().getName());
        }

        // Resolve genre names
        if (profile.getGenres() != null) {
            response.setGenreNames(profile.getGenres().stream()
                    .map(Genre::getName)
                    .collect(Collectors.toSet()));
        }

        // Resolve language name
        if (profile.getDefaultLanguageId() != null) {
            Language language = languageRepository.findById(profile.getDefaultLanguageId()).orElse(null);
            if (language != null) {
                response.setDefaultLanguageName(language.getName());
            }
        }

        return response;
    }
}
package com.platform.user.service;

import com.platform.user.dto.*;
import com.platform.user.entity.*;
import com.platform.user.repository.*;
import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
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
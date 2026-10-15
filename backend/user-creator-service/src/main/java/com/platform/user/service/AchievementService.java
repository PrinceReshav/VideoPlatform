package com.platform.user.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import com.platform.user.entity.AchievementDefinition;
import com.platform.user.entity.CreatorProfile;
import com.platform.user.entity.UserAchievement;
import com.platform.user.entity.Channel;
import com.platform.user.repository.AchievementDefinitionRepository;
import com.platform.user.repository.ChannelRepository;
import com.platform.user.repository.CreatorProfileRepository;
import com.platform.user.repository.UserAchievementRepository;
import com.platform.user.dto.AchievementDefinitionDTO;
import com.platform.user.dto.UserAchievementDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling creator achievements (Feature 29: Creator Achievements)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementDefinitionRepository achievementDefinitionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Check and award achievements for the current user based on their activity
     */
    @Transactional
    public List<UserAchievementDTO> checkAndAwardAchievements() {
        UUID userId = currentUserAccessor.getCurrentUserId();

        // Get the creator profile for the user
        CreatorProfile creatorProfile = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CREATOR_PROFILE_NOT_FOUND, "Creator profile not found"));

        // Get the channel for the user
        Channel channel = channelRepository.findByCreatorProfileId(creatorProfile.getId())
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.CHANNEL_NOT_FOUND, "Channel not found"));

        // Get all active achievement definitions
        List<AchievementDefinition> activeAchievements = achievementDefinitionRepository.findByActiveTrue();

        // Get achievements already earned by the user
        List<UserAchievement> existingAchievements = userAchievementRepository.findByUserId(userId);
        Set<UUID> earnedAchievementIds = existingAchievements.stream()
                .map(UserAchievement::getAchievementDefinitionId)
                .collect(Collectors.toSet());

        // Check each achievement to see if the user qualifies
        List<UserAchievement> newlyEarnedAchievements = activeAchievements.stream()
                .filter(achievement -> !earnedAchievementIds.contains(achievement.getId()))
                .filter(achievement -> userQualifiesForAchievement(userId, achievement))
                .map(achievement -> createUserAchievement(userId, achievement))
                .collect(Collectors.toList());

        // Save newly earned achievements
        if (!newlyEarnedAchievements.isEmpty()) {
            newlyEarnedAchievements = userAchievementRepository.saveAll(newlyEarnedAchievements);
            log.info("Awarded {} new achievements to user {}", newlyEarnedAchievements.size(), userId);
        }

        // Convert to DTOs
        return newlyEarnedAchievements.stream()
                .map(this::mapToUserAchievementDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if a user qualifies for a specific achievement
     */
    private boolean userQualifiesForAchievement(UUID userId, AchievementDefinition achievement) {
        switch (achievement.getCategory()) {
            case "SUBSCRIBERS":
                return checkSubscriberCountAchievement(userId, achievement);
            case "VIEWS":
                return checkViewCountAchievement(userId, achievement);
            case "VIDEOS":
                return checkVideoCountAchievement(userId, achievement);
            case "LIKES":
                return checkLikeCountAchievement(userId, achievement);
            case "COMMENTS":
                return checkCommentCountAchievement(userId, achievement);
            default:
                return false;
        }
    }

    /**
     * Check subscriber count based achievements
     */
    private boolean checkSubscriberCountAchievement(UUID userId, AchievementDefinition achievement) {
        CreatorProfile creatorProfile = creatorProfileRepository.findByUserId(userId)
                .orElse(null);
        if (creatorProfile == null) {
            return false;
        }

        long subscriberCount = creatorProfile.getSubscriberCount();
        return subscriberCount >= achievement.getThresholdValue();
    }

    /**
     * Check view count based achievements
     */
    private boolean checkViewCountAchievement(UUID userId, AchievementDefinition achievement) {
        // Get total views across all videos by the creator
        Long totalViews = videoRepository.getTotalViewsByCreatorId(userId);
        if (totalViews == null) {
            totalViews = 0L;
        }

        return totalViews >= achievement.getThresholdValue();
    }

    /**
     * Check video count based achievements
     */
    private boolean checkVideoCountAchievement(UUID userId, AchievementDefinition achievement) {
        int videoCount = videoRepository.countByCreatorId(userId);
        return videoCount >= achievement.getThresholdValue();
    }

    /**
     * Check like count based achievements
     */
    private boolean checkLikeCountAchievement(UUID userId, AchievementDefinition achievement) {
        // Get total likes across all videos by the creator
        Long totalLikes = videoRepository.getTotalLikesByCreatorId(userId);
        if (totalLikes == null) {
            totalLikes = 0L;
        }

        return totalLikes >= achievement.getThresholdValue();
    }

    /**
     * Check comment count based achievements
     */
    private boolean checkCommentCountAchievement(UUID userId, AchievementDefinition achievement) {
        // Get total comments across all videos by the creator
        Long totalComments = videoRepository.getTotalCommentsByCreatorId(userId);
        if (totalComments == null) {
            totalComments = 0L;
        }

        return totalComments >= achievement.getThresholdValue();
    }

    /**
     * Create a user achievement record when an achievement is earned
     */
    private UserAchievement createUserAchievement(UUID userId, AchievementDefinition achievement) {
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUserId(userId);
        userAchievement.setAchievementDefinitionId(achievement.getId());
        userAchievement.setEarnedAt(Instant.now());

        // Set the value when earned based on achievement type
        switch (achievement.getCategory()) {
            case "SUBSCRIBERS":
                CreatorProfile creatorProfile = creatorProfileRepository.findByUserId(userId).orElse(null);
                userAchievement.setValueWhenEarned(creatorProfile != null ? creatorProfile.getSubscriberCount() : 0);
                break;
            case "VIEWS":
                Long totalViews = videoRepository.getTotalViewsByCreatorId(userId);
                userAchievement.setValueWhenEarned(totalViews != null ? totalViews : 0);
                break;
            case "VIDEOS":
                userAchievement.setValueWhenEarned(videoRepository.countByCreatorId(userId));
                break;
            case "LIKES":
                Long totalLikes = videoRepository.getTotalLikesByCreatorId(userId);
                userAchievement.setValueWhenEarned(totalLikes != null ? totalLikes : 0);
                break;
            case "COMMENTS":
                Long totalComments = videoRepository.getTotalCommentsByCreatorId(userId);
                userAchievement.setValueWhenEarned(totalComments != null ? totalComments : 0);
                break;
            default:
                userAchievement.setValueWhenEarned(0);
        }

        return userAchievement;
    }

    /**
     * Get all achievements earned by the current user
     */
    @Transactional(readOnly = true)
    public List<UserAchievementDTO> getUserAchievements() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);
        return userAchievements.stream()
                .map(this::mapToUserAchievementDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all available achievement definitions
     */
    @Transactional(readOnly = true)
    public List<AchievementDefinitionDTO> getAvailableAchievements() {
        List<AchievementDefinition> definitions = achievementDefinitionRepository.findByActiveTrue();
        return definitions.stream()
                .map(this::mapToAchievementDefinitionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert AchievementDefinition entity to AchievementDefinitionDTO
     */
    private AchievementDefinitionDTO mapToAchievementDefinitionDTO(AchievementDefinition definition) {
        return AchievementDefinitionDTO.builder()
                .id(definition.getId())
                .achievementCode(definition.getAchievementCode())
                .name(definition.getName())
                .description(definition.getDescription())
                .category(definition.getCategory())
                .achievementType(definition.getAchievementType())
                .thresholdValue(definition.getThresholdValue())
                .points(definition.getPoints())
                .active(definition.isActive())
                .iconUrl(definition.getIconUrl())
                .color(definition.getColor())
                .createdAt(definition.getCreatedAt())
                .updatedAt(definition.getUpdatedAt())
                .build();
    }

    /**
     * Convert UserAchievement entity to UserAchievementDTO
     */
    private UserAchievementDTO mapToUserAchievementDTO(UserAchievement userAchievement) {
        AchievementDefinition achievement = userAchievement.getAchievementDefinitionId() != null ?
                achievementDefinitionRepository.findById(userAchievement.getAchievementDefinitionId()).orElse(null) : null;

        return UserAchievementDTO.builder()
                .id(userAchievement.getId())
                .userId(userAchievement.getUserId())
                .achievementDefinitionId(userAchievement.getAchievementDefinitionId())
                .achievementCode(achievement != null ? achievement.getAchievementCode() : null)
                .achievementName(achievement != null ? achievement.getName() : null)
                .achievementDescription(achievement != null ? achievement.getDescription() : null)
                .achievementCategory(achievement != null ? achievement.getCategory() : null)
                .valueWhenEarned(userAchievement.getValueWhenEarned())
                .earnedAt(userAchievement.getEarnedAt())
                .build();
    }
}
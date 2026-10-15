package com.platform.user.repository;

import com.platform.user.entity.UserAchievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserAchievement entity (Feature 29: Creator Achievements)
 */
@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    /** Find achievements earned by a user */
    List<UserAchievement> findByUserId(UUID userId);

    /** Find users who have earned a specific achievement */
    List<UserAchievement> findByAchievementDefinitionId(UUID achievementDefinitionId);

    /** Check if a user has earned a specific achievement */
    Optional<UserAchievement> findByUserIdAndAchievementDefinitionId(UUID userId, UUID achievementDefinitionId);

    /** Find user achievements with pagination */
    Page<UserAchievement> findByUserId(UUID userId, Pageable pageable);
}
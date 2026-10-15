package com.platform.user.repository;

import com.platform.user.entity.AchievementDefinition;
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
 * Repository for AchievementDefinition entity (Feature 29: Creator Achievements)
 */
@Repository
public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, UUID> {

    /** Find achievement definitions by category */
    List<AchievementDefinition> findByCategory(String category);

    /** Find active achievement definitions */
    List<AchievementDefinition> findByActiveTrue();

    /** Find achievement definition by code */
    Optional<AchievementDefinition> findByAchievementCode(String achievementCode);

    /** Find achievement definitions by category with pagination */
    Page<AchievementDefinition> findByCategory(String category, Pageable pageable);
}
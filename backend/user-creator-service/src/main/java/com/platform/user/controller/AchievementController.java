package com.platform.user.controller;

import com.platform.user.dto.AchievementDefinitionDTO;
import com.platform.user.dto.UserAchievementDTO;
import com.platform.user.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for creator achievements (Feature 29: Creator Achievements).
 */
@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    /**
     * Check and award achievements for the current user.
     */
    @GetMapping("/check")
    public ResponseEntity<List<UserAchievementDTO>>
    checkAndAwardAchievements() {

        List<UserAchievementDTO> newAchievements =
                achievementService.checkAndAwardAchievements();

        return ResponseEntity.ok(newAchievements);
    }

    /**
     * Get all achievements earned by the current user.
     */
    @GetMapping("/earned")
    public ResponseEntity<List<UserAchievementDTO>>
    getUserAchievements() {

        List<UserAchievementDTO> achievements =
                achievementService.getUserAchievements();

        return ResponseEntity.ok(achievements);
    }

    /**
     * Get all available achievement definitions.
     */
    @GetMapping("/available")
    public ResponseEntity<List<AchievementDefinitionDTO>>
    getAvailableAchievements() {

        List<AchievementDefinitionDTO> achievements =
                achievementService.getAvailableAchievements();

        return ResponseEntity.ok(achievements);
    }
}
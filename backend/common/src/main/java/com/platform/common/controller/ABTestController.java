package com.platform.common.controller;

import com.platform.common.dto.ABTestDTO;
import com.platform.common.dto.ABTestParticipantDTO;
import com.platform.common.dto.ABTestVariationDTO;
import com.platform.common.service.ABTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for A/B tests (Feature 30: A/B Testing)
 */
@RestController
@RequestMapping("/api/v1/ab-tests")
@RequiredArgsConstructor
public class ABTestController {

    private final ABTestService abTestService;

    /**
     * Create a new A/B test
     */
    @PostMapping
    public ResponseEntity<ABTestDTO> createABTest(
            @Valid @RequestBody ABTestDTO testDTO) {
        ABTestDTO response = abTestService.createABTest(testDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a variation to an A/B test
     */
    @PostMapping("/{testId}/variations")
    public ResponseEntity<ABTestVariationDTO> addVariation(
            @PathVariable UUID testId,
            @Valid @RequestBody ABTestVariationDTO variationDTO) {
        ABTestVariationDTO response = abTestService.addVariation(testId, variationDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Assign the current user to a variation in an A/B test
     */
    @PostMapping("/{testId}/assign")
    public ResponseEntity<ABTestParticipantDTO> assignUserToTest(
            @PathVariable UUID testId) {
        ABTestParticipantDTO response = abTestService.assignUserToTest(testId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the variation assigned to the current user for a specific test
     */
    @GetMapping("/{testId}/variation")
    public ResponseEntity<ABTestVariationDTO> getUserVariation(
            @PathVariable UUID testId) {
        ABTestVariationDTO response = abTestService.getUserVariation(testId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active A/B tests
     */
    @GetMapping("/active")
    public ResponseEntity<List<ABTestDTO>> getActiveTests() {
        List<ABTestDTO> tests = abTestService.getActiveTests();
        return ResponseEntity.ok(tests);
    }

    /**
     * Get all running A/B tests (within start/end times)
     */
    @GetMapping("/running")
    public ResponseEntity<List<ABTestDTO>> getRunningTests() {
        List<ABTestDTO> tests = abTestService.getRunningTests();
        return ResponseEntity.ok(tests);
    }
}
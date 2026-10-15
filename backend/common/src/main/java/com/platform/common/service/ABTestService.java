package com.platform.common.service;

import com.platform.common.dto.ABTestDTO;
import com.platform.common.dto.ABTestParticipantDTO;
import com.platform.common.dto.ABTestVariationDTO;
import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.entity.ABTest;
import com.platform.common.entity.ABTestParticipant;
import com.platform.common.entity.ABTestVariation;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.repository.ABTestParticipantRepository;
import com.platform.common.repository.ABTestRepository;
import com.platform.common.repository.ABTestVariationRepository;
import com.platform.common.security.CurrentUserAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service for handling A/B tests.
 *
 * Feature 30: A/B Testing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ABTestService {

    private final ABTestRepository abTestRepository;
    private final ABTestVariationRepository abTestVariationRepository;
    private final ABTestParticipantRepository abTestParticipantRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Create a new A/B test.
     */
    @Transactional
    public ABTestDTO createABTest(ABTestDTO testDTO) {

        if (testDTO == null) {
            throw badRequest("A/B test request cannot be null");
        }

        if (testDTO.getName() == null || testDTO.getName().isBlank()) {
            throw badRequest("A/B test name is required");
        }

        if (abTestRepository.findByName(testDTO.getName()).isPresent()) {
            throw badRequest(
                    "A/B test with this name already exists"
            );
        }

        int trafficPercentage =
                testDTO.getTrafficPercentage() != null
                        ? clampPercentage(testDTO.getTrafficPercentage())
                        : 100;

        if (testDTO.getStartTime() != null
                && testDTO.getEndTime() != null
                && testDTO.getStartTime().isAfter(testDTO.getEndTime())) {

            throw badRequest(
                    "A/B test start time cannot be after end time"
            );
        }

        ABTest test = new ABTest();

        test.setName(testDTO.getName());
        test.setDescription(testDTO.getDescription());
        test.setFeatureArea(testDTO.getFeatureArea());
        test.setActive(testDTO.isActive());
        test.setStartTime(testDTO.getStartTime());
        test.setEndTime(testDTO.getEndTime());
        test.setTrafficPercentage(trafficPercentage);

        /*
         * createdAt and updatedAt are inherited from Auditable
         * and are populated by Spring Data auditing.
         *
         * Do not manually set them here.
         */
        ABTest savedTest = abTestRepository.save(test);

        log.info(
                "Created A/B test: {} ({})",
                savedTest.getName(),
                savedTest.getId()
        );

        return mapToDTO(savedTest);
    }

    /**
     * Add a variation to an A/B test.
     */
    @Transactional
    public ABTestVariationDTO addVariation(
            UUID testId,
            ABTestVariationDTO variationDTO) {

        if (testId == null) {
            throw badRequest("A/B test ID is required");
        }

        if (variationDTO == null) {
            throw badRequest("Variation request cannot be null");
        }

        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() ->
                        new NotFoundException(
                                ApiErrorCodes.AB_TEST_NOT_FOUND,
                                "A/B test not found: " + testId
                        )
                );

        if (variationDTO.getName() == null
                || variationDTO.getName().isBlank()) {

            throw badRequest("Variation name is required");
        }

        if (abTestVariationRepository
                .findByAbTestIdAndName(
                        testId,
                        variationDTO.getName()
                )
                .isPresent()) {

            throw badRequest(
                    "Variation with this name already exists in this test"
            );
        }

        int trafficAllocation =
                variationDTO.getTrafficAllocation() != null
                        ? Math.max(
                        0,
                        variationDTO.getTrafficAllocation()
                )
                        : 0;

        ABTestVariation variation = new ABTestVariation();

        variation.setAbTestId(testId);
        variation.setName(variationDTO.getName());
        variation.setDescription(variationDTO.getDescription());

        /*
         * ABTestVariation contains:
         *
         * private boolean isControl;
         *
         * Therefore Lombok generates:
         *
         * isControl()
         * setControl(...)
         *
         * NOT setIsControl(...)
         */
        variation.setControl(variationDTO.isControl());

        variation.setTrafficAllocation(trafficAllocation);
        variation.setConfiguration(variationDTO.getConfiguration());

        /*
         * active is primitive boolean.
         * Therefore there is no null check.
         */
        variation.setActive(variationDTO.isActive());

        ABTestVariation savedVariation =
                abTestVariationRepository.save(variation);

        log.info(
                "Added variation {} to A/B test {}",
                savedVariation.getName(),
                test.getName()
        );

        return mapToVariationDTO(savedVariation);
    }

    /**
     * Assign the current authenticated user to a variation.
     */
    @Transactional
    public ABTestParticipantDTO assignUserToTest(UUID testId) {

        if (testId == null) {
            throw badRequest("A/B test ID is required");
        }

        UUID userId = currentUserAccessor.getCurrentUserId();

        if (userId == null) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user is required"
            );
        }

        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() ->
                        new NotFoundException(
                                ApiErrorCodes.AB_TEST_NOT_FOUND,
                                "A/B test not found: " + testId
                        )
                );

        Instant now = Instant.now();

        /*
         * Verify that the test is currently running.
         */
        if (!test.isActive()) {
            throw badRequest(
                    "A/B test is not currently active"
            );
        }

        if (test.getStartTime() != null
                && test.getStartTime().isAfter(now)) {

            throw badRequest(
                    "A/B test has not started yet"
            );
        }

        if (test.getEndTime() != null
                && test.getEndTime().isBefore(now)) {

            throw badRequest(
                    "A/B test has already ended"
            );
        }

        /*
         * Prevent assigning the same user twice.
         */
        if (abTestParticipantRepository
                .findByUserIdAndAbTestId(
                        userId,
                        testId
                )
                .isPresent()) {

            throw badRequest(
                    "User is already assigned to this A/B test"
            );
        }

        List<ABTestVariation> variations =
                abTestVariationRepository
                        .findByAbTestIdAndActiveTrue(testId);

        if (variations.isEmpty()) {
            throw badRequest(
                    "No active variations found for this test"
            );
        }

        ABTestVariation selectedVariation =
                assignUserToVariation(
                        variations,
                        test.getTrafficPercentage()
                );

        /*
         * User was outside the test traffic percentage.
         */
        if (selectedVariation == null) {
            throw badRequest(
                    "User was not included in this A/B test traffic allocation"
            );
        }

        ABTestParticipant participant =
                new ABTestParticipant();

        participant.setUserId(userId);
        participant.setAbTestId(testId);
        participant.setAbTestVariationId(
                selectedVariation.getId()
        );
        participant.setAssignedAt(now);
        participant.setActive(true);

        ABTestParticipant savedParticipant =
                abTestParticipantRepository.save(participant);

        log.info(
                "Assigned user {} to variation {} in A/B test {}",
                userId,
                selectedVariation.getName(),
                test.getName()
        );

        return mapToParticipantDTO(savedParticipant);
    }

    /**
     * Get the variation assigned to the current user.
     */
    @Transactional(readOnly = true)
    public ABTestVariationDTO getUserVariation(UUID testId) {

        if (testId == null) {
            throw badRequest("A/B test ID is required");
        }

        UUID userId = currentUserAccessor.getCurrentUserId();

        if (userId == null) {
            throw new BusinessException(
                    ApiErrorCodes.ACCESS_DENIED,
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user is required"
            );
        }

        ABTestParticipant participant =
                abTestParticipantRepository
                        .findByUserIdAndAbTestId(
                                userId,
                                testId
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        ApiErrorCodes.AB_TEST_NOT_FOUND,
                                        "User is not assigned to this A/B test"
                                )
                        );

        ABTestVariation variation =
                abTestVariationRepository
                        .findById(
                                participant.getAbTestVariationId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        ApiErrorCodes.RESOURCE_NOT_FOUND,
                                        "A/B test variation not found"
                                )
                        );

        return mapToVariationDTO(variation);
    }

    /**
     * Get all active A/B tests.
     */
    @Transactional(readOnly = true)
    public List<ABTestDTO> getActiveTests() {

        return abTestRepository
                .findByActiveTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all currently running A/B tests.
     */
    @Transactional(readOnly = true)
    public List<ABTestDTO> getRunningTests() {

        return abTestRepository
                .findRunningTests(Instant.now())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Select a variation using:
     *
     * 1. Overall test traffic percentage.
     * 2. Variation traffic allocation.
     */
    private ABTestVariation assignUserToVariation(
            List<ABTestVariation> variations,
            Integer trafficPercentage) {

        if (variations == null || variations.isEmpty()) {
            return null;
        }

        int traffic =
                trafficPercentage == null
                        ? 100
                        : clampPercentage(trafficPercentage);

        /*
         * No users should enter a 0% test.
         */
        if (traffic == 0) {
            return null;
        }

        /*
         * Overall test traffic sampling.
         */
        int trafficRoll =
                ThreadLocalRandom.current().nextInt(100);

        if (trafficRoll >= traffic) {
            return null;
        }

        /*
         * Calculate total variation allocation.
         */
        int totalAllocation = variations
                .stream()
                .map(ABTestVariation::getTrafficAllocation)
                .filter(Objects::nonNull)
                .mapToInt(value -> Math.max(0, value))
                .sum();

        /*
         * If no allocation has been configured,
         * distribute users equally.
         */
        if (totalAllocation <= 0) {

            return variations.get(
                    ThreadLocalRandom.current()
                            .nextInt(variations.size())
            );
        }

        /*
         * Weighted random selection.
         */
        int randomPoint =
                ThreadLocalRandom.current()
                        .nextInt(totalAllocation);

        int cumulativeAllocation = 0;

        for (ABTestVariation variation : variations) {

            int allocation =
                    variation.getTrafficAllocation() == null
                            ? 0
                            : Math.max(
                            0,
                            variation.getTrafficAllocation()
                    );

            cumulativeAllocation += allocation;

            if (randomPoint < cumulativeAllocation) {
                return variation;
            }
        }

        /*
         * Defensive fallback.
         */
        return variations.get(
                variations.size() - 1
        );
    }

    /**
     * Convert ABTest entity to DTO.
     */
    private ABTestDTO mapToDTO(ABTest test) {

        return ABTestDTO
                .builder()
                .id(test.getId())
                .name(test.getName())
                .description(test.getDescription())
                .featureArea(test.getFeatureArea())
                .active(test.isActive())
                .startTime(test.getStartTime())
                .endTime(test.getEndTime())
                .trafficPercentage(
                        test.getTrafficPercentage()
                )
                .lastCalculatedAt(
                        test.getLastCalculatedAt()
                )
                .createdAt(
                        test.getCreatedAt()
                )
                .updatedAt(
                        test.getUpdatedAt()
                )
                .build();
    }

    /**
     * Convert ABTestVariation entity to DTO.
     */
    private ABTestVariationDTO mapToVariationDTO(
            ABTestVariation variation) {

        return ABTestVariationDTO
                .builder()
                .id(variation.getId())
                .abTestId(variation.getAbTestId())
                .name(variation.getName())
                .description(variation.getDescription())
                .isControl(variation.isControl())
                .trafficAllocation(
                        variation.getTrafficAllocation()
                )
                .configuration(
                        variation.getConfiguration()
                )
                .active(
                        variation.isActive()
                )
                .createdAt(
                        variation.getCreatedAt()
                )
                .updatedAt(
                        variation.getUpdatedAt()
                )
                .build();
    }

    /**
     * Convert participant entity to DTO.
     */
    private ABTestParticipantDTO mapToParticipantDTO(
            ABTestParticipant participant) {

        return ABTestParticipantDTO
                .builder()
                .id(participant.getId())
                .userId(participant.getUserId())
                .abTestId(participant.getAbTestId())
                .abTestVariationId(
                        participant.getAbTestVariationId()
                )
                .assignedAt(
                        participant.getAssignedAt()
                )
                .active(
                        participant.isActive()
                )
                .createdAt(
                        participant.getCreatedAt()
                )
                .updatedAt(
                        participant.getUpdatedAt()
                )
                .build();
    }

    /**
     * Create a correctly constructed BusinessException.
     *
     * BusinessException requires:
     *
     * (code, HttpStatus, message)
     */
    private BusinessException badRequest(String message) {

        return new BusinessException(
                ApiErrorCodes.BAD_REQUEST,
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    /**
     * Keep percentages inside 0-100.
     */
    private int clampPercentage(int value) {

        return Math.max(
                0,
                Math.min(100, value)
        );
    }
}
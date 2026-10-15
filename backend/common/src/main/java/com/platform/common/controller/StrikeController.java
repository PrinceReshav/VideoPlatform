package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.dto.StrikeRequest;
import com.platform.common.dto.StrikeResponse;
import com.platform.common.entity.Strike;
import com.platform.common.service.StrikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for strike management (Feature 18: Reporting/Strikes/Trust System)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/strikes")
@RequiredArgsConstructor
public class StrikeController {

    private final StrikeService strikeService;

    /**
     * Create a new strike
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StrikeResponse>> createStrike(
            @Valid @RequestBody StrikeRequest request) {

        // Note: In a real application, you would set issuedBy from authentication context
        // For now, we'll leave it null and set it in the service if needed
        var strike = strikeService.createStrike(Strike.builder()
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .reason(request.getReason())
                .details(request.getDetails())
                .issuedAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .build());

        StrikeResponse response = strikeService.mapToResponse(strike);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get a strike by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StrikeResponse>> getStrikeById(
            @PathVariable UUID id) {

        Strike strike = strikeService.getStrikeById(id);
        StrikeResponse response = strikeService.mapToResponse(strike);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get strikes by target ID and target type
     */
    @GetMapping("/target/{targetId}/{targetType}")
    public ResponseEntity<ApiResponse<List<StrikeResponse>>> getStrikesByTargetIdAndTargetType(
            @PathVariable UUID targetId,
            @PathVariable String targetType) {

        List<Strike> strikes = strikeService.getStrikesByTargetIdAndTargetType(targetId, targetType);
        List<StrikeResponse> responses = strikeService.mapToResponseList(strikes);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get active strikes by target ID and target type
     */
    @GetMapping("/target/{targetId}/{targetType}/active")
    public ResponseEntity<ApiResponse<List<StrikeResponse>>> getActiveStrikesByTargetIdAndTargetType(
            @PathVariable UUID targetId,
            @PathVariable String targetType) {

        List<Strike> strikes = strikeService.getActiveStrikesByTargetIdAndTargetType(targetId, targetType);
        List<StrikeResponse> responses = strikeService.mapToResponseList(strikes);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get strikes by reason
     */
    @GetMapping("/reason/{reason}")
    public ResponseEntity<ApiResponse<List<StrikeResponse>>> getStrikesByReason(
            @PathVariable String reason) {

        List<Strike> strikes = strikeService.getStrikesByReason(reason);
        List<StrikeResponse> responses = strikeService.mapToResponseList(strikes);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get strikes by timestamp range
     */
    @GetMapping("/timestamp-range")
    public ResponseEntity<ApiResponse<List<StrikeResponse>>> getStrikesByTimestampRange(
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {

        List<Strike> strikes = strikeService.getStrikesByTimestampRange(startTime, endTime);
        List<StrikeResponse> responses = strikeService.mapToResponseList(strikes);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get strikes with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StrikeResponse>>> getStrikesByCriteria(
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) UUID issuedBy,
            @RequestParam(required = false) UUID reportId,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime) {

        List<Strike> strikes = strikeService.getStrikesByCriteria(
                targetId, targetType, reason, isActive, issuedBy, reportId, startTime, endTime);
        List<StrikeResponse> responses = strikeService.mapToResponseList(strikes);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Count active strikes by target ID and target type
     */
    @GetMapping("/target/{targetId}/{targetType}/count")
    public ResponseEntity<ApiResponse<Long>> countActiveStrikesByTargetIdAndTargetType(
            @PathVariable UUID targetId,
            @PathVariable String targetType) {

        long count = strikeService.countActiveStrikesByTargetIdAndTargetType(targetId, targetType);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Create a strike from an approved report (internal use)
     */
    @PostMapping("/from-report/{reportId}")
    public ResponseEntity<ApiResponse<Void>> createStrikeFromApprovedReport(
            @PathVariable UUID reportId) {

        strikeService.createStrikeFromApprovedReport(reportId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.dto.CopyrightClaimRequest;
import com.platform.common.dto.CopyrightClaimResponse;
import com.platform.common.entity.CopyrightClaim;
import com.platform.common.service.CopyrightClaimService;
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
 * Controller for copyright claim management (Feature 3: Copyright Detection & Claims System)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/copyright-claims")
@RequiredArgsConstructor
public class CopyrightClaimController {

    private final CopyrightClaimService copyrightClaimService;

    /**
     * Create a new copyright claim
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CopyrightClaimResponse>> createCopyrightClaim(
            @Valid @RequestBody CopyrightClaimRequest request) {

        CopyrightClaim claim = CopyrightClaim.builder()
                .videoId(request.getVideoId())
                .claimantId(request.getClaimantId())
                .rightsOwnerId(request.getRightsOwnerId())
                .copyrightedWorkDescription(request.getCopyrightedWorkDescription())
                .copyrightRegistrationNumber(request.getCopyrightRegistrationNumber())
                .originalWorkUrl(request.getOriginalWorkUrl())
                .infringementTimestamp(request.getInfringementTimestamp())
                .infringementDuration(request.getInfringementDuration())
                .infringementType(request.getInfringementType())
                .evidence(request.getEvidence())
                .expiresAt(request.getExpiresAt())
                .status("PENDING")
                .actionTaken("NONE")
                .legallyValidated(false)
                .build();

        CopyrightClaim savedClaim = copyrightClaimService.createCopyrightClaim(claim);
        CopyrightClaimResponse response = mapToResponse(savedClaim);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get a copyright claim by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CopyrightClaimResponse>> getCopyrightClaimById(
            @PathVariable UUID id) {

        CopyrightClaim claim = copyrightClaimService.getCopyrightClaimById(id);
        CopyrightClaimResponse response = mapToResponse(claim);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all copyright claims for a video
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<CopyrightClaimResponse>>> getCopyrightClaimsByVideoId(
            @PathVariable UUID videoId) {

        List<CopyrightClaim> claims = copyrightClaimService.getCopyrightClaimsByVideoId(videoId);
        List<CopyrightClaimResponse> responses = claims.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get all copyright claims by a claimant
     */
    @GetMapping("/claimant/{claimantId}")
    public ResponseEntity<ApiResponse<List<CopyrightClaimResponse>>> getCopyrightClaimsByClaimantId(
            @PathVariable UUID claimantId) {

        List<CopyrightClaim> claims = copyrightClaimService.getCopyrightClaimsByClaimantId(claimantId);
        List<CopyrightClaimResponse> responses = claims.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get all copyright claims by a rights owner
     */
    @GetMapping("/rights-owner/{rightsOwnerId}")
    public ResponseEntity<ApiResponse<List<CopyrightClaimResponse>>> getCopyrightClaimsByRightsOwnerId(
            @PathVariable UUID rightsOwnerId) {

        List<CopyrightClaim> claims = copyrightClaimService.getCopyrightClaimsByRightsOwnerId(rightsOwnerId);
        List<CopyrightClaimResponse> responses = claims.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get copyright claims by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<CopyrightClaimResponse>>> getCopyrightClaimsByStatus(
            @PathVariable String status) {

        List<CopyrightClaim> claims = copyrightClaimService.getCopyrightClaimsByStatus(status);
        List<CopyrightClaimResponse> responses = claims.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Update a copyright claim
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CopyrightClaimResponse>> updateCopyrightClaim(
            @PathVariable UUID id,
            @Valid @RequestBody CopyrightClaimRequest request) {

        CopyrightClaim claimDetails = CopyrightClaim.builder()
                .videoId(request.getVideoId())
                .claimantId(request.getClaimantId())
                .rightsOwnerId(request.getRightsOwnerId())
                .copyrightedWorkDescription(request.getCopyrightedWorkDescription())
                .copyrightRegistrationNumber(request.getCopyrightRegistrationNumber())
                .originalWorkUrl(request.getOriginalWorkUrl())
                .infringementTimestamp(request.getInfringementTimestamp())
                .infringementDuration(request.getInfringementDuration())
                .infringementType(request.getInfringementType())
                .evidence(request.getEvidence())
                .expiresAt(request.getExpiresAt())
                .build();

        CopyrightClaim updatedClaim = copyrightClaimService.updateCopyrightClaim(id, claimDetails);
        CopyrightClaimResponse response = mapToResponse(updatedClaim);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a copyright claim
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCopyrightClaim(
            @PathVariable UUID id) {

        copyrightClaimService.deleteCopyrightClaim(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Map CopyrightClaim entity to CopyrightClaimResponse DTO
     */
    private CopyrightClaimResponse mapToResponse(CopyrightClaim claim) {
        return CopyrightClaimResponse.builder()
                .id(claim.getId())
                .videoId(claim.getVideoId())
                .claimantId(claim.getClaimantId())
                .rightsOwnerId(claim.getRightsOwnerId())
                .copyrightedWorkDescription(claim.getCopyrightedWorkDescription())
                .copyrightRegistrationNumber(claim.getCopyrightRegistrationNumber())
                .originalWorkUrl(claim.getOriginalWorkUrl())
                .infringementTimestamp(claim.getInfringementTimestamp())
                .infringementDuration(claim.getInfringementDuration())
                .infringementType(claim.getInfringementType())
                .status(claim.getStatus())
                .actionTaken(claim.getActionTaken())
                .legallyValidated(claim.isLegallyValidated())
                .evidence(claim.getEvidence())
                .creatorResponse(claim.getCreatorResponse())
                .filedAt(claim.getFiledAt())
                .updatedAt(claim.getUpdatedAt())
                .expiresAt(claim.getExpiresAt())
                .build();
    }
}
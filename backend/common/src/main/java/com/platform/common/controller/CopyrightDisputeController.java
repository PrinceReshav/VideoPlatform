package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.dto.CopyrightDisputeRequest;
import com.platform.common.dto.CopyrightDisputeResponse;
import com.platform.common.entity.CopyrightDispute;
import com.platform.common.service.CopyrightDisputeService;
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
 * Controller for copyright dispute management (Feature 3: Copyright Detection & Claims System)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/copyright-disputes")
@RequiredArgsConstructor
public class CopyrightDisputeController {

    private final CopyrightDisputeService copyrightDisputeService;

    /**
     * Create a new copyright dispute
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CopyrightDisputeResponse>> createCopyrightDispute(
            @Valid @RequestBody CopyrightDisputeRequest request) {

        CopyrightDispute dispute = CopyrightDispute.builder()
                .copyrightClaimId(request.getCopyrightClaimId())
                .disputantId(request.getDisputantId())
                .disputeReason(request.getDisputeReason())
                .explanation(request.getExplanation())
                .disputeType(request.getDisputeType())
                .evidence(request.getEvidence())
                .expiresAt(request.getExpiresAt())
                .status("PENDING")
                .actionTaken("NONE")
                .legallyReviewed(false)
                .build();

        CopyrightDispute savedDispute = copyrightDisputeService.createCopyrightDispute(dispute);
        CopyrightDisputeResponse response = mapToResponse(savedDispute);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get a copyright dispute by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CopyrightDisputeResponse>> getCopyrightDisputeById(
            @PathVariable UUID id) {

        CopyrightDispute dispute = copyrightDisputeService.getCopyrightDisputeById(id);
        CopyrightDisputeResponse response = mapToResponse(dispute);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all copyright disputes for a copyright claim
     */
    @GetMapping("/claim/{copyrightClaimId}")
    public ResponseEntity<ApiResponse<List<CopyrightDisputeResponse>>> getCopyrightDisputesByClaimId(
            @PathVariable UUID copyrightClaimId) {

        List<CopyrightDispute> disputes = copyrightDisputeService.getCopyrightDisputesByClaimId(copyrightClaimId);
        List<CopyrightDisputeResponse> responses = disputes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get all copyright disputes by a disputant
     */
    @GetMapping("/disputant/{disputantId}")
    public ResponseEntity<ApiResponse<List<CopyrightDisputeResponse>>> getCopyrightDisputesByDisputantId(
            @PathVariable UUID disputantId) {

        List<CopyrightDispute> disputes = copyrightDisputeService.getCopyrightDisputesByDisputantId(disputantId);
        List<CopyrightDisputeResponse> responses = disputes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get copyright disputes by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<CopyrightDisputeResponse>>> getCopyrightDisputesByStatus(
            @PathVariable String status) {

        List<CopyrightDispute> disputes = copyrightDisputeService.getCopyrightDisputesByStatus(status);
        List<CopyrightDisputeResponse> responses = disputes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get copyright disputes by dispute type
     */
    @GetMapping("/type/{disputeType}")
    public ResponseEntity<ApiResponse<List<CopyrightDisputeResponse>>> getCopyrightDisputesByDisputeType(
            @PathVariable String disputeType) {

        List<CopyrightDispute> disputes = copyrightDisputeService.getCopyrightDisputesByDisputeType(disputeType);
        List<CopyrightDisputeResponse> responses = disputes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Update a copyright dispute
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CopyrightDisputeResponse>> updateCopyrightDispute(
            @PathVariable UUID id,
            @Valid @RequestBody CopyrightDisputeRequest request) {

        CopyrightDispute disputeDetails = CopyrightDispute.builder()
                .copyrightClaimId(request.getCopyrightClaimId())
                .disputantId(request.getDisputantId())
                .disputeReason(request.getDisputeReason())
                .explanation(request.getExplanation())
                .disputeType(request.getDisputeType())
                .evidence(request.getEvidence())
                .expiresAt(request.getExpiresAt())
                .build();

        CopyrightDispute updatedDispute = copyrightDisputeService.updateCopyrightDispute(id, disputeDetails);
        CopyrightDisputeResponse response = mapToResponse(updatedDispute);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a copyright dispute
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCopyrightDispute(
            @PathVariable UUID id) {

        copyrightDisputeService.deleteCopyrightDispute(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Map CopyrightDispute entity to CopyrightDisputeResponse DTO
     */
    private CopyrightDisputeResponse mapToResponse(CopyrightDispute dispute) {
        return CopyrightDisputeResponse.builder()
                .id(dispute.getId())
                .copyrightClaimId(dispute.getCopyrightClaimId())
                .disputantId(dispute.getDisputantId())
                .disputeReason(dispute.getDisputeReason())
                .explanation(dispute.getExplanation())
                .disputeType(dispute.getDisputeType())
                .status(dispute.getStatus())
                .actionTaken(dispute.getActionTaken())
                .legallyReviewed(dispute.isLegallyReviewed())
                .evidence(dispute.getEvidence())
                .claimantResponse(dispute.getClaimantResponse())
                .filedAt(dispute.getFiledAt())
                .updatedAt(dispute.getUpdatedAt())
                .expiresAt(dispute.getExpiresAt())
                .build();
    }
}
package com.platform.user.controller;

import com.platform.user.dto.ApiResponse;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.user.entity.VerificationRequest;
import com.platform.user.dto.VerificationRequestDTO;
import com.platform.user.dto.VerificationResponseDTO;
import com.platform.user.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for verification requests (Feature 19: Creator Verification)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Create a new verification request
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> createVerificationRequest(
            @Valid @RequestBody VerificationRequestDTO requestDTO) {

        UUID userId = currentUserAccessor.getCurrentUserId();


        VerificationRequest request = new VerificationRequest();
        request.setUserId(userId);
        request.setRequestType(requestDTO.getRequestType());
        request.setRequestedTier(requestDTO.getRequestedTier());
        request.setRequestReason(requestDTO.getRequestReason());
        request.setAdditionalInfo(requestDTO.getAdditionalInfo());


        VerificationRequest savedRequest = verificationService.createVerificationRequest(request);
        VerificationResponseDTO responseDTO = verificationService.mapToResponseDTO(savedRequest);

        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    /**
     * Get a verification request by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> getVerificationRequestById(
            @PathVariable UUID id) {

        VerificationRequest request = verificationService.getVerificationRequestById(id);
        VerificationResponseDTO responseDTO = verificationService.mapToResponseDTO(request);

        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    /**
     * Get verification requests by user ID
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("#userId == principal.userId or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VerificationResponseDTO>>> getVerificationRequestsByUserId(
            @PathVariable UUID userId) {

        List<VerificationRequest> requests = verificationService.getVerificationRequestsByUserId(userId);
        List<VerificationResponseDTO> responseDTOs = verificationService.mapToResponseDTOList(requests);

        return ResponseEntity.ok(ApiResponse.success(responseDTOs));
    }

    /**
     * Get verification requests by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VerificationResponseDTO>>> getVerificationRequestsByStatus(
            @PathVariable String status) {

        List<VerificationRequest> requests = verificationService.getVerificationRequestsByStatus(status);
        List<VerificationResponseDTO> responseDTOs = verificationService.mapToResponseDTOList(requests);

        return ResponseEntity.ok(ApiResponse.success(responseDTOs));
    }

    /**
     * Get verification requests with filtering and pagination
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VerificationResponseDTO>>> getVerificationRequestsByCriteria(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestedTier,
            @RequestParam(required = false) UUID reviewedBy,
            @RequestParam(required = false) Boolean processed,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime) {

        List<VerificationRequest> requests = verificationService.getVerificationRequestsByCriteria(
                userId, requestType, status, requestedTier, reviewedBy, processed, startTime, endTime);
        List<VerificationResponseDTO> responseDTOs = verificationService.mapToResponseDTOList(requests);

        return ResponseEntity.ok(ApiResponse.success(responseDTOs));
    }

    /**
     * Review and approve/reject a verification request
     */
    @PostMapping("/{requestId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> reviewVerificationRequest(
            @PathVariable UUID requestId,
            @RequestParam(required = false) UUID reviewerId,
            @RequestParam boolean approve,
            @RequestParam String feedback) {

        UUID effectiveReviewerId = currentUserAccessor.getCurrentUserId();
        VerificationRequest request = verificationService.reviewVerificationRequest(requestId, effectiveReviewerId, approve, feedback);
        VerificationResponseDTO responseDTO = verificationService.mapToResponseDTO(request);

        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }
}
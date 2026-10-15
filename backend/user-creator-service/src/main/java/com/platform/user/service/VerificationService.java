package com.platform.user.service;

import com.platform.user.dto.ApiResponse;
import com.platform.user.dto.VerificationRequestDTO;
import com.platform.user.dto.VerificationResponseDTO;
import com.platform.user.entity.Channel;
import com.platform.user.entity.CreatorProfile;
import com.platform.user.entity.VerificationRequest;
import com.platform.user.repository.ChannelRepository;
import com.platform.user.repository.CreatorProfileRepository;
import com.platform.user.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling verification requests (Feature 19: Creator Verification)
 */
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final ChannelRepository channelRepository;
    private final CreatorProfileRepository creatorProfileRepository;

    /**
     * Create a new verification request
     */
    @Transactional
    public VerificationRequest createVerificationRequest(VerificationRequest request) {
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());
        VerificationRequest savedRequest = verificationRequestRepository.save(request);
        return savedRequest;
    }

    /**
     * Get a verification request by ID
     */
    public VerificationRequest getVerificationRequestById(UUID id) {
        return verificationRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Verification request not found with id: " + id));
    }

    /**
     * Get verification requests by user ID
     */
    public List<VerificationRequest> getVerificationRequestsByUserId(UUID userId) {
        return verificationRequestRepository.findByUserId(userId);
    }

    /**
     * Get verification requests by status
     */
    public List<VerificationRequest> getVerificationRequestsByStatus(String status) {
        return verificationRequestRepository.findByStatus(status);
    }

    /**
     * Get verification requests with filtering and pagination
     */
    public List<VerificationRequest> getVerificationRequestsByCriteria(
            UUID userId,
            String requestType,
            String status,
            String requestedTier,
            UUID reviewedBy,
            Boolean processed,
            Instant startTime,
            Instant endTime) {
        return verificationRequestRepository.findByCriteria(userId, requestType, status, requestedTier, reviewedBy, processed, startTime, endTime, null)
                .getContent();
    }

    /**
     * Review and approve/reject a verification request
     */
    @Transactional
    public VerificationRequest reviewVerificationRequest(UUID requestId, UUID reviewerId, boolean approve, String feedback) {
        VerificationRequest request = getVerificationRequestById(requestId);

        // Only allow reviewing pending requests
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Only pending verification requests can be reviewed");
        }

        // Update request with review information
        request.setReviewedBy(reviewerId);
        request.setReviewedAt(Instant.now());
        request.setReviewFeedback(feedback);
        request.setProcessed(true);

        if (approve) {
            request.setStatus("APPROVED");
            // Apply verification to the channel or creator profile
            applyVerification(request);
        } else {
            request.setStatus("REJECTED");
        }

        request.setUpdatedAt(Instant.now());
        return verificationRequestRepository.save(request);
    }

    /**
     * Apply verification to channel or creator profile when request is approved
     */
    private void applyVerification(VerificationRequest request) {
        UUID userId = request.getUserId();

        // Apply verification to channel if requested
        if ("CHANNEL".equals(request.getRequestType()) || "BOTH".equals(request.getRequestType())) {
            Channel channel = channelRepository.findByCreatorProfileId(
                    creatorProfileRepository.findByUserId(userId)
                            .orElseThrow(() -> new IllegalArgumentException("Creator profile not found for user: " + userId))
                            .getId())
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found for user: " + userId));

            channel.setVerified(true);
            channel.setVerifiedAt(Instant.now());
            channel.setVerifiedBy(request.getReviewedBy());
            channel.setVerificationTier(request.getRequestedTier());
            channelRepository.save(channel);
        }

        // Apply verification to creator profile if requested
        if ("CREATOR_PROFILE".equals(request.getRequestType()) || "BOTH".equals(request.getRequestType())) {
            CreatorProfile creatorProfile = creatorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Creator profile not found for user: " + userId));

            creatorProfile.setVerified(true);
            creatorProfile.setVerifiedAt(Instant.now());
            creatorProfile.setVerifiedBy(request.getReviewedBy());
            creatorProfileRepository.save(creatorProfile);
        }
    }

    /**
     * Convert VerificationRequest entity to VerificationResponseDTO
     */
    public VerificationResponseDTO mapToResponseDTO(VerificationRequest request) {
        return VerificationResponseDTO.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .requestedTier(request.getRequestedTier())
                .requestReason(request.getRequestReason())
                .additionalInfo(request.getAdditionalInfo())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .reviewFeedback(request.getReviewFeedback())
                .processed(request.isProcessed())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    /**
     * Convert list of VerificationRequest entities to list of VerificationResponseDTOs
     */
    public List<VerificationResponseDTO> mapToResponseDTOList(List<VerificationRequest> requests) {
        return requests.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
}
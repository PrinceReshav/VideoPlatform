package com.platform.common.service;

import com.platform.common.entity.CopyrightClaim;
import com.platform.common.repository.CopyrightClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling copyright claims (Feature 3: Copyright Detection & Claims System)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyrightClaimService {

    private final CopyrightClaimRepository copyrightClaimRepository;

    /**
     * Create a new copyright claim
     */
    @Transactional
    public CopyrightClaim createCopyrightClaim(CopyrightClaim claim) {
        claim.setFiledAt(Instant.now());
        claim.setUpdatedAt(Instant.now());
        CopyrightClaim savedClaim = copyrightClaimRepository.save(claim);
        log.info("Created copyright claim {} for video {} by claimant {}",
                savedClaim.getId(), savedClaim.getVideoId(), savedClaim.getClaimantId());
        return savedClaim;
    }

    /**
     * Get a copyright claim by ID
     */
    public CopyrightClaim getCopyrightClaimById(UUID id) {
        return copyrightClaimRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Copyright claim not found with ID: " + id));
    }

    /**
     * Get all copyright claims for a video
     */
    public List<CopyrightClaim> getCopyrightClaimsByVideoId(UUID videoId) {
        return copyrightClaimRepository.findByVideoId(videoId);
    }

    /**
     * Get all copyright claims by a claimant
     */
    public List<CopyrightClaim> getCopyrightClaimsByClaimantId(UUID claimantId) {
        return copyrightClaimRepository.findByClaimantId(claimantId);
    }

    /**
     * Get all copyright claims by a rights owner
     */
    public List<CopyrightClaim> getCopyrightClaimsByRightsOwnerId(UUID rightsOwnerId) {
        return copyrightClaimRepository.findByRightsOwnerId(rightsOwnerId);
    }

    /**
     * Get copyright claims by status
     */
    public List<CopyrightClaim> getCopyrightClaimsByStatus(String status) {
        return copyrightClaimRepository.findByStatus(status);
    }

    /**
     * Update a copyright claim
     */
    @Transactional
    public CopyrightClaim updateCopyrightClaim(UUID id, CopyrightClaim claimDetails) {
        CopyrightClaim claim = getCopyrightClaimById(id);

        // Update fields (except immutable ones like ID, filedAt, etc.)
        claim.setClaimantId(claimDetails.getClaimantId());
        claim.setRightsOwnerId(claimDetails.getRightsOwnerId());
        claim.setCopyrightedWorkDescription(claimDetails.getCopyrightedWorkDescription());
        claim.setCopyrightRegistrationNumber(claimDetails.getCopyrightRegistrationNumber());
        claim.setOriginalWorkUrl(claimDetails.getOriginalWorkUrl());
        claim.setInfringementTimestamp(claimDetails.getInfringementTimestamp());
        claim.setInfringementDuration(claimDetails.getInfringementDuration());
        claim.setInfringementType(claimDetails.getInfringementType());
        claim.setStatus(claimDetails.getStatus());
        claim.setActionTaken(claimDetails.getActionTaken());
        claim.setLegallyValidated(claimDetails.isLegallyValidated());
        claim.setEvidence(claimDetails.getEvidence());
        claim.setCreatorResponse(claimDetails.getCreatorResponse());
        claim.setUpdatedAt(Instant.now());
        claim.setExpiresAt(claimDetails.getExpiresAt());

        CopyrightClaim updatedClaim = copyrightClaimRepository.save(claim);
        log.info("Updated copyright claim {}", updatedClaim.getId());
        return updatedClaim;
    }

    /**
     * Delete a copyright claim (soft delete)
     */
    @Transactional
    public void deleteCopyrightClaim(UUID id) {
        CopyrightClaim claim = getCopyrightClaimById(id);
        copyrightClaimRepository.delete(claim);
        log.info("Deleted copyright claim {}", id);
    }

    /**
     * Get copyright claims with filtering and pagination
     */
    public List<CopyrightClaim> getCopyrightClaimsByCriteria(
            UUID videoId,
            UUID claimantId,
            UUID rightsOwnerId,
            String status,
            String infringementType,
            Instant startTime,
            Instant endTime) {

        return copyrightClaimRepository.findByCriteria(
                videoId, claimantId, rightsOwnerId, status, infringementType,
                startTime, endTime,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
    }
}
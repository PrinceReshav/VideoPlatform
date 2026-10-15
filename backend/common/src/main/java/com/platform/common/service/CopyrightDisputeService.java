package com.platform.common.service;

import com.platform.common.entity.CopyrightDispute;
import com.platform.common.repository.CopyrightDisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling copyright disputes (Feature 3: Copyright Detection & Claims System)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyrightDisputeService {

    private final CopyrightDisputeRepository copyrightDisputeRepository;

    /**
     * Create a new copyright dispute
     */
    @Transactional
    public CopyrightDispute createCopyrightDispute(CopyrightDispute dispute) {
        dispute.setFiledAt(Instant.now());
        dispute.setUpdatedAt(Instant.now());
        CopyrightDispute savedDispute = copyrightDisputeRepository.save(dispute);
        log.info("Created copyright dispute {} for claim {} by disputant {}",
                savedDispute.getId(), savedDispute.getCopyrightClaimId(), savedDispute.getDisputantId());
        return savedDispute;
    }

    /**
     * Get a copyright dispute by ID
     */
    public CopyrightDispute getCopyrightDisputeById(UUID id) {
        return copyrightDisputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Copyright dispute not found with ID: " + id));
    }

    /**
     * Get all copyright disputes for a copyright claim
     */
    public List<CopyrightDispute> getCopyrightDisputesByClaimId(UUID copyrightClaimId) {
        return copyrightDisputeRepository.findByCopyrightClaimId(copyrightClaimId);
    }

    /**
     * Get all copyright disputes by a disputant
     */
    public List<CopyrightDispute> getCopyrightDisputesByDisputantId(UUID disputantId) {
        return copyrightDisputeRepository.findByDisputantId(disputantId);
    }

    /**
     * Get copyright disputes by status
     */
    public List<CopyrightDispute> getCopyrightDisputesByStatus(String status) {
        return copyrightDisputeRepository.findByStatus(status);
    }

    /**
     * Get copyright disputes by dispute type
     */
    public List<CopyrightDispute> getCopyrightDisputesByDisputeType(String disputeType) {
        return copyrightDisputeRepository.findByDisputeType(disputeType);
    }

    /**
     * Update a copyright dispute
     */
    @Transactional
    public CopyrightDispute updateCopyrightDispute(UUID id, CopyrightDispute disputeDetails) {
        CopyrightDispute dispute = getCopyrightDisputeById(id);

        // Update fields (except immutable ones like ID, filedAt, etc.)
        dispute.setCopyrightClaimId(disputeDetails.getCopyrightClaimId());
        dispute.setDisputantId(disputeDetails.getDisputantId());
        dispute.setDisputeReason(disputeDetails.getDisputeReason());
        dispute.setExplanation(disputeDetails.getExplanation());
        dispute.setDisputeType(disputeDetails.getDisputeType());
        dispute.setStatus(disputeDetails.getStatus());
        dispute.setActionTaken(disputeDetails.getActionTaken());
        dispute.setLegallyReviewed(disputeDetails.isLegallyReviewed());
        dispute.setEvidence(disputeDetails.getEvidence());
        dispute.setClaimantResponse(disputeDetails.getClaimantResponse());
        dispute.setUpdatedAt(Instant.now());
        dispute.setExpiresAt(disputeDetails.getExpiresAt());

        CopyrightDispute updatedDispute = copyrightDisputeRepository.save(dispute);
        log.info("Updated copyright dispute {}", updatedDispute.getId());
        return updatedDispute;
    }

    /**
     * Delete a copyright dispute (soft delete)
     */
    @Transactional
    public void deleteCopyrightDispute(UUID id) {
        CopyrightDispute dispute = getCopyrightDisputeById(id);
        copyrightDisputeRepository.delete(dispute);
        log.info("Deleted copyright dispute {}", id);
    }

    /**
     * Get copyright disputes with filtering and pagination
     */
    public List<CopyrightDispute> getCopyrightDisputesByCriteria(
            UUID copyrightClaimId,
            UUID disputantId,
            String status,
            String disputeType,
            Instant startTime,
            Instant endTime) {

        return copyrightDisputeRepository.findByCriteria(
                copyrightClaimId, disputantId, status, disputeType,
                startTime, endTime,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
    }
}
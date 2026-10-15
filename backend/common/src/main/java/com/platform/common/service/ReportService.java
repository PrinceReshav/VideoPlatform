package com.platform.common.service;

import com.platform.common.entity.Report;
import com.platform.common.entity.Strike;
import com.platform.common.repository.ReportRepository;
import com.platform.common.repository.StrikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling reports (Feature 18: Reporting/Strikes/Trust System)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final StrikeRepository strikeRepository;

    /**
     * Create a new report
     */
    @Transactional
    public Report createReport(Report report) {
        report.setCreatedAt(Instant.now());
        report.setUpdatedAt(Instant.now());
        Report savedReport = reportRepository.save(report);
        log.info("Created report {} by user {} against {} {} of type {}",
                savedReport.getId(), savedReport.getReporterId(),
                savedReport.getTargetType(), savedReport.getTargetId(),
                savedReport.getReason());
        return savedReport;
    }

    /**
     * Get a report by ID
     */
    public Report getReportById(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + id));
    }

    /**
     * Get reports by reporter ID
     */
    public List<Report> getReportsByReporterId(UUID reporterId) {
        return reportRepository.findByReporterId(reporterId);
    }

    /**
     * Get reports by target ID and target type
     */
    public List<Report> getReportsByTargetIdAndTargetType(UUID targetId, String targetType) {
        return reportRepository.findByTargetIdAndTargetType(targetId, targetType);
    }

    /**
     * Get reports by reason
     */
    public List<Report> getReportsByReason(String reason) {
        return reportRepository.findByReason(reason);
    }

    /**
     * Get reports by status
     */
    public List<Report> getReportsByStatus(String status) {
        return reportRepository.findByStatus(status);
    }

    /**
     * Get reports by timestamp range
     */
    public List<Report> getReportsByTimestampRange(Instant startTime, Instant endTime) {
        return reportRepository.findByCreatedAtBetween(startTime, endTime);
    }

    /**
     * Update a report
     */
    @Transactional
    public Report updateReport(UUID id, Report reportDetails) {
        Report report = getReportById(id);

        // Update fields (except immutable ones like ID, reporterId, createdAt, etc.)
        report.setTargetId(reportDetails.getTargetId());
        report.setTargetType(reportDetails.getTargetType());
        report.setReason(reportDetails.getReason());
        report.setDescription(reportDetails.getDescription());
        report.setEvidence(reportDetails.getEvidence());
        report.setStatus(reportDetails.getStatus());
        report.setActionTaken(reportDetails.getActionTaken());
        report.setStrikesAssigned(reportDetails.getStrikesAssigned());
        report.setReviewedBy(reportDetails.getReviewedBy());
        report.setUpdatedAt(Instant.now());
        report.setReviewedAt(reportDetails.getReviewedAt());

        Report updatedReport = reportRepository.save(report);
        log.info("Updated report {}", updatedReport.getId());

        // Automatically create strike if report is approved and no strikes assigned yet
        if ("APPROVED".equals(report.getStatus()) && report.getStrikesAssigned() == 0) {
            createStrikeFromApprovedReport(report.getId());
        }

        return updatedReport;
    }

    /**
     * Delete a report (soft delete)
     */
    @Transactional
    public void deleteReport(UUID id) {
        Report report = getReportById(id);
        reportRepository.delete(report);
        log.info("Deleted report {}", id);
    }

    /**
     * Get reports with filtering and pagination
     */
    public List<Report> getReportsByCriteria(
            UUID reporterId,
            UUID targetId,
            String targetType,
            String reason,
            String status,
            Instant startTime,
            Instant endTime) {

        return reportRepository.findByCriteria(
                reporterId, targetId, targetType, reason, status,
                startTime, endTime,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
    }

    /**
     * Automatically create a strike when a report is approved
     */
    @Transactional
    public void createStrikeFromApprovedReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + reportId));

        // Only create strike for approved reports
        if (!"APPROVED".equals(report.getStatus())) {
            return;
        }

        // Check if strike already exists for this report
        if (report.getStrikesAssigned() > 0) {
            return; // Strike already assigned
        }

        // Determine number of strikes based on existing strikes
        long existingStrikes = strikeRepository.findByTargetIdAndTargetTypeAndIsActiveTrue(
                report.getTargetId(), report.getTargetType()).size();
        int strikesToAssign = Math.min(1, 3 - (int) existingStrikes); // Assign 1 strike, max 3 total

        if (strikesToAssign <= 0) {
            return; // Already at maximum strikes
        }

        // Create strike
        Strike strike = Strike.builder()
                .targetId(report.getTargetId())
                .targetType(report.getTargetType())
                .reason(report.getReason())
                .details("Strike assigned from approved report: " + report.getDescription())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(30L * 24 * 60 * 60)) // 30 days expiration
                .issuedBy(report.getReviewedBy()) // Moderator who approved the report
                .reportId(report.getId())
                .build();

        strikeRepository.save(strike);

        // Update report with strike information
        report.setStrikesAssigned((int) existingStrikes + strikesToAssign);
        report.setActionTaken("STRIKE");
        reportRepository.save(report);
    }
}
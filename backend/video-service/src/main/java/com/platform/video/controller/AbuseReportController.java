package com.platform.video.controller;

import com.platform.video.dto.AbuseReportRequest;
import com.platform.video.dto.AbuseReportResponse;
import com.platform.video.entity.AbuseReport;
import com.platform.video.service.AbuseReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for abuse reports (Feature 33: Anti-Bot/Anti-Abuse System)
 */
@RestController
@RequestMapping("/api/v1/abuse-reports")
@RequiredArgsConstructor
public class AbuseReportController {

    private final AbuseReportService abuseReportService;

    /**
     * Create a new abuse report
     */
    @PostMapping
    public ResponseEntity<AbuseReportResponse> createAbuseReport(
            @Valid @RequestBody AbuseReportRequest request) {
        AbuseReportResponse response = abuseReportService.createAbuseReport(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get an abuse report by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AbuseReportResponse> getAbuseReport(@PathVariable UUID id) {
        AbuseReportResponse response = abuseReportService.getAbuseReport(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get abuse reports by reporter ID
     */
    @GetMapping("/reporter/{reporterId}")
    public ResponseEntity<List<AbuseReportResponse>> getAbuseReportsByReporter(@PathVariable UUID reporterId) {
        List<AbuseReportResponse> responses = abuseReportService.getAbuseReportsByReporter(reporterId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get abuse reports that need review (for moderators)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AbuseReportResponse>> getPendingAbuseReports() {
        List<AbuseReportResponse> responses = abuseReportService.getPendingAbuseReports();
        return ResponseEntity.ok(responses);
    }

    /**
     * Review and resolve an abuse report (moderator action)
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<AbuseReportResponse> reviewAbuseReport(
            @PathVariable UUID id,
            @RequestParam String reviewNotes,
            @RequestParam String status) {
        AbuseReport.AbuseReportStatus abuseReportStatus = AbuseReport.AbuseReportStatus.valueOf(status);
        AbuseReportResponse response = abuseReportService.reviewAbuseReport(id, reviewNotes, abuseReportStatus);
        return ResponseEntity.ok(response);
    }

    /**
     * Get abuse reports by video ID
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<AbuseReportResponse>> getAbuseReportsByVideo(@PathVariable UUID videoId) {
        List<AbuseReportResponse> responses = abuseReportService.getAbuseReportsByVideo(videoId);
        return ResponseEntity.ok(responses);
    }
}
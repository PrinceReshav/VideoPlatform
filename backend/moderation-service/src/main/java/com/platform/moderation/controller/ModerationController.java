package com.platform.moderation.controller;

import com.platform.common.dto.PageResponse;
import com.platform.moderation.dto.CreateReportRequest;
import com.platform.moderation.dto.ModerationActionResponse;
import com.platform.moderation.dto.ReportResponse;
import com.platform.moderation.dto.ReviewDecisionRequest;
import com.platform.moderation.entity.Report;
import com.platform.moderation.service.ModerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/reports")
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody CreateReportRequest request) {
        ReportResponse response = moderationService.createReport(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/my")
    public ResponseEntity<PageResponse<ReportResponse>> getMyReports(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(moderationService.getMyReports(page, size));
    }

    @GetMapping("/reports/pending")
    public ResponseEntity<PageResponse<ReportResponse>> getPendingReports(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(moderationService.getPendingReports(page, size));
    }

    @GetMapping("/reports")
    public ResponseEntity<PageResponse<ReportResponse>> getAllReports(
            @RequestParam(required = false) Report.Status status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(moderationService.getAllReports(status, page, size));
    }

    @PutMapping("/reports/{reportId}/review")
    public ResponseEntity<ReportResponse> reviewReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        ReportResponse response = moderationService.reviewReport(reportId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/actions")
    public ResponseEntity<PageResponse<ModerationActionResponse>> getModerationActions(
            @RequestParam UUID moderatorId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(moderationService.getModerationActions(moderatorId, page, size));
    }
}
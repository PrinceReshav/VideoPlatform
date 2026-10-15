package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.dto.ReportRequest;
import com.platform.common.dto.ReportResponse;
import com.platform.common.entity.Report;
import com.platform.common.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for report management (Feature 18: Reporting/Strikes/Trust System)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Create a new report
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody ReportRequest request) {

        Report report = Report.builder()
                .reporterId(request.getReporterId())
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .reason(request.getReason())
                .description(request.getDescription())
                .evidence(request.getEvidence())
                .build();

        Report savedReport = reportService.createReport(report);
        ReportResponse response = mapToResponse(savedReport);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get a report by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(
            @PathVariable UUID id) {

        Report report = reportService.getReportById(id);
        ReportResponse response = mapToResponse(report);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get reports by reporter ID
     */
    @GetMapping("/reporter/{reporterId}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByReporterId(
            @PathVariable UUID reporterId) {

        List<Report> reports = reportService.getReportsByReporterId(reporterId);
        List<ReportResponse> responses = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get reports by target ID and target type
     */
    @GetMapping("/target/{targetId}/{targetType}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByTargetIdAndTargetType(
            @PathVariable UUID targetId,
            @PathVariable String targetType) {

        List<Report> reports = reportService.getReportsByTargetIdAndTargetType(targetId, targetType);
        List<ReportResponse> responses = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get reports by reason
     */
    @GetMapping("/reason/{reason}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByReason(
            @PathVariable String reason) {

        List<Report> reports = reportService.getReportsByReason(reason);
        List<ReportResponse> responses = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get reports by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByStatus(
            @PathVariable String status) {

        List<Report> reports = reportService.getReportsByStatus(status);
        List<ReportResponse> responses = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get reports by timestamp range
     */
    @GetMapping("/timestamp-range")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByTimestampRange(
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {

        List<Report> reports = reportService.getReportsByTimestampRange(startTime, endTime);
        List<ReportResponse> responses = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Update a report
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReport(
            @PathVariable UUID id,
            @Valid @RequestBody ReportRequest request) {

        Report reportDetails = Report.builder()
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .reason(request.getReason())
                .description(request.getDescription())
                .evidence(request.getEvidence())
                .build();

        Report updatedReport = reportService.updateReport(id, reportDetails);
        ReportResponse response = mapToResponse(updatedReport);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a report
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(
            @PathVariable UUID id) {

        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get reports with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByCriteria(
            @RequestParam(required = false) UUID reporterId,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // For simplicity, we'll get all matching results without pagination in this example
        // In a real implementation, you'd want to add pagination support to the service method
        List<Report> reports = reportService.getReportsByCriteria(
                reporterId, targetId, targetType, reason, status, startTime, endTime);
        List<ReportResponse> responses = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Map Report entity to ReportResponse DTO
     */
    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .targetId(report.getTargetId())
                .targetType(report.getTargetType())
                .reason(report.getReason())
                .description(report.getDescription())
                .evidence(report.getEvidence())
                .status(report.getStatus())
                .actionTaken(report.getActionTaken())
                .strikesAssigned(report.getStrikesAssigned())
                .reviewedBy(report.getReviewedBy())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .reviewedAt(report.getReviewedAt())
                .build();
    }
}
package com.platform.moderation.controller;

import com.platform.common.dto.PageResponse;
import com.platform.moderation.dto.ReportResponse;
import com.platform.moderation.dto.ReviewDecisionRequest;
import com.platform.moderation.entity.Report;
import com.platform.moderation.service.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
public class AdminModerationController {
    private final ModerationService moderationService;

    @GetMapping
    public ResponseEntity<PageResponse<ReportResponse>> getReports(
            @RequestParam(required=false) Report.Status status,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok(moderationService.getAllReports(status, page, size));
    }

    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<ReportResponse> resolve(@PathVariable UUID reportId, @RequestBody Map<String,String> body) {
        String raw = body.getOrDefault("action", "APPROVE").toUpperCase(Locale.ROOT);
        ReviewDecisionRequest.Decision decision;
        try { decision = ReviewDecisionRequest.Decision.valueOf(raw); }
        catch (IllegalArgumentException e) { decision = ReviewDecisionRequest.Decision.APPROVE; }
        ReviewDecisionRequest request = ReviewDecisionRequest.builder()
                .decision(decision)
                .reviewNotes(body.get("notes"))
                .actionTaken(raw)
                .build();
        return ResponseEntity.ok(moderationService.reviewReport(reportId, request));
    }
}

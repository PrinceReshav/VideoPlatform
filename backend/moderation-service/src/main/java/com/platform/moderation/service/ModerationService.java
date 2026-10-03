package com.platform.moderation.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.event.ModerationActionAppliedEvent;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.moderation.dto.CreateReportRequest;
import com.platform.moderation.dto.ModerationActionResponse;
import com.platform.moderation.dto.ReportResponse;
import com.platform.moderation.dto.ReviewDecisionRequest;
import com.platform.moderation.entity.ModerationAction;
import com.platform.moderation.entity.Report;
import com.platform.moderation.repository.ModerationActionRepository;
import com.platform.moderation.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ReportRepository reportRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setStatus(Report.Status.PENDING);
        report = reportRepository.save(report);

        log.info("Created report {} by user {} for {}:{}", report.getId(), userId, request.getTargetType(), request.getTargetId());
        return mapToReportResponse(report);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getMyReports(int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportRepository.findByReporterId(userId, pageable);

        return PageResponse.of(
                reports.getContent().stream().map(this::mapToReportResponse).toList(),
                page, size, reports.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getPendingReports(int page, int size) {
        if (!currentUserAccessor.isAdmin() && !currentUserAccessor.hasRole(com.platform.common.security.PlatformUserDetails.Role.MODERATOR)) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Admin or moderator access required");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportRepository.findByStatus(Report.Status.PENDING, pageable);

        return PageResponse.of(
                reports.getContent().stream().map(this::mapToReportResponse).toList(),
                page, size, reports.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getAllReports(Report.Status status, int page, int size) {
        if (!currentUserAccessor.isAdmin() && !currentUserAccessor.hasRole(com.platform.common.security.PlatformUserDetails.Role.MODERATOR)) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Admin or moderator access required");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports;

        if (status != null) {
            reports = reportRepository.findByStatus(status, pageable);
        } else {
            reports = reportRepository.findAll(pageable);
        }

        return PageResponse.of(
                reports.getContent().stream().map(this::mapToReportResponse).toList(),
                page, size, reports.getTotalElements()
        );
    }

    @Transactional
    public ReportResponse reviewReport(UUID reportId, ReviewDecisionRequest request) {
        if (!currentUserAccessor.isAdmin() && !currentUserAccessor.hasRole(com.platform.common.security.PlatformUserDetails.Role.MODERATOR)) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Admin or moderator access required");
        }

        UUID moderatorId = currentUserAccessor.getCurrentUserId();
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Report not found"));

        if (report.getStatus() == Report.Status.RESOLVED || report.getStatus() == Report.Status.DISMISSED) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_CONFLICT,
                    org.springframework.http.HttpStatus.CONFLICT, "Report already reviewed");
        }

        switch (request.getDecision()) {
            case APPROVE -> {
                // Content is fine, dismiss report
                report.setStatus(Report.Status.DISMISSED);
                report.setActionTaken("No action needed");
            }
            case REJECT -> {
                // Take action on content
                report.setStatus(Report.Status.RESOLVED);
                report.setActionTaken(request.getActionTaken());
                takeModerationAction(report, request, moderatorId);
            }
            case ESCALATE -> {
                report.setStatus(Report.Status.ESCALATED);
                report.setActionTaken("Escalated to senior moderator");
            }
        }

        report.setReviewedBy(moderatorId);
        report.setReviewedAt(Instant.now());
        report.setReviewNotes(request.getReviewNotes());
        report = reportRepository.save(report);

        log.info("Report {} reviewed by moderator {} with decision {}", reportId, moderatorId, request.getDecision());
        return mapToReportResponse(report);
    }

    private void takeModerationAction(Report report, ReviewDecisionRequest request, UUID moderatorId) {
        ModerationAction action = new ModerationAction();
        action.setReportId(report.getId());
        action.setModeratorId(moderatorId);
        // Convert Report.TargetType to ModerationAction.TargetType
        action.setTargetType(ModerationAction.TargetType.valueOf(report.getTargetType().name()));
        action.setTargetId(report.getTargetId());

        // Map action taken string to ActionType
        action.setActionType(mapActionType(request.getActionTaken()));
        action.setReason(request.getReviewNotes());
        action.setDurationHours(request.getDurationHours());
        if (request.getDurationHours() != null) {
            action.setExpiresAt(Instant.now().plusSeconds(request.getDurationHours() * 3600));
        }

        moderationActionRepository.save(action);

        ModerationActionAppliedEvent event = ModerationActionAppliedEvent.builder()
                .reportId(report.getId())
                .moderatorId(moderatorId)
                .targetType(report.getTargetType().name())
                .targetId(report.getTargetId())
                .actionType(action.getActionType().name())
                .reason(action.getReason())
                .durationHours(action.getDurationHours())
                .expiresAt(action.getExpiresAt())
                .timestamp(Instant.now())
                .build();
        kafkaTemplate.send(KafkaTopicNames.MODERATION_ACTION_APPLIED, report.getTargetId().toString(),
                EventEnvelope.create("moderation.action.applied", "v1", event, "moderation-service"));
    }

    private ModerationAction.ActionType mapActionType(String actionTaken) {
        if (actionTaken == null) return ModerationAction.ActionType.WARN;

        return switch (actionTaken.toLowerCase()) {
            case "warn" -> ModerationAction.ActionType.WARN;
            case "hide_content" -> ModerationAction.ActionType.HIDE_CONTENT;
            case "delete_content" -> ModerationAction.ActionType.DELETE_CONTENT;
            case "temporary_suspension" -> ModerationAction.ActionType.TEMPORARY_SUSPENSION;
            case "permanent_suspension" -> ModerationAction.ActionType.PERMANENT_SUSPENSION;
            case "channel_strike" -> ModerationAction.ActionType.CHANNEL_STRIKE;
            case "channel_termination" -> ModerationAction.ActionType.CHANNEL_TERMINATION;
            case "remove_strike" -> ModerationAction.ActionType.REMOVE_STRIKE;
            case "restore_content" -> ModerationAction.ActionType.RESTORE_CONTENT;
            default -> ModerationAction.ActionType.WARN;
        };
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationActionResponse> getModerationActions(UUID moderatorId, int page, int size) {
        if (!currentUserAccessor.isAdmin() && !moderatorId.equals(currentUserAccessor.getCurrentUserId())) {
            throw new BusinessException(ApiErrorCodes.ACCESS_DENIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ModerationAction> actions = moderationActionRepository.findByModeratorId(moderatorId, pageable);

        return PageResponse.of(
                actions.getContent().stream().map(this::mapToActionResponse).toList(),
                page, size, actions.getTotalElements()
        );
    }

    private ReportResponse mapToReportResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .reporterUsername(null)
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(null)
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .reviewedBy(report.getReviewedBy())
                .reviewedAt(report.getReviewedAt())
                .reviewNotes(report.getReviewNotes())
                .actionTaken(report.getActionTaken())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private ModerationActionResponse mapToActionResponse(ModerationAction action) {
        return ModerationActionResponse.builder()
                .id(action.getId())
                .reportId(action.getReportId())
                .moderatorId(action.getModeratorId())
                .moderatorUsername(null)
                .targetType(action.getTargetType())
                .targetId(action.getTargetId())
                .actionType(action.getActionType())
                .reason(action.getReason())
                .durationHours(action.getDurationHours())
                .expiresAt(action.getExpiresAt())
                .createdAt(action.getCreatedAt())
                .build();
    }
}
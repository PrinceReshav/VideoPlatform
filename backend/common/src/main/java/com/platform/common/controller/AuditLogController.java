package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.entity.AuditLog;
import com.platform.common.repository.AuditLogRepository;
import com.platform.common.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller for audit log management.
 *
 * Feature 38: Security & Audit Logs.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    /**
     * Get audit logs with filtering and pagination.
     *
     * Supported filters:
     * - userId
     * - actionType
     * - entityType
     * - entityId
     * - startTime
     * - endTime
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize
        );

        /*
         * If entityId is supplied, use the existing repository method:
         *
         * List<AuditLog> findByEntityTypeAndEntityId(
         *      String entityType,
         *      UUID entityId
         * )
         *
         * The current repository does not expose a paginated version,
         * therefore pagination is performed safely in memory.
         */
        if (entityId != null) {

            List<AuditLog> entityLogs =
                    auditLogRepository.findByEntityTypeAndEntityId(
                            entityType,
                            entityId
                    );

            Page<AuditLog> result =
                    toPage(entityLogs, pageable);

            return ResponseEntity.ok(
                    ApiResponse.success(result)
            );
        }

        boolean hasFilters =
                userId != null
                        || actionType != null
                        || entityType != null
                        || startTime != null
                        || endTime != null;

        Page<AuditLog> auditLogs;

        if (hasFilters) {

            /*
             * The repository criteria query expects non-null
             * timestamp parameters, so provide sensible bounds
             * when only one side of the range is supplied.
             */
            Instant effectiveStart =
                    startTime != null
                            ? startTime
                            : Instant.EPOCH;

            Instant effectiveEnd =
                    endTime != null
                            ? endTime
                            : Instant.now();

            auditLogs =
                    auditLogRepository.findByCriteria(
                            userId,
                            actionType,
                            entityType,
                            effectiveStart,
                            effectiveEnd,
                            pageable
                    );

        } else {

            auditLogs =
                    auditLogRepository.findAllBy(
                            pageable
                    );
        }

        return ResponseEntity.ok(
                ApiResponse.success(auditLogs)
        );
    }

    /**
     * Get audit log by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuditLog>> getAuditLogById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return auditLogRepository.findById(id)
                .map(auditLog ->
                        ResponseEntity.ok(
                                ApiResponse.success(auditLog)
                        )
                )
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    /**
     * Get audit logs for a specific user.
     *
     * The existing repository returns List<AuditLog>, so this
     * controller converts that list into a proper Page.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize
        );

        List<AuditLog> auditLogs =
                auditLogRepository.findByUserId(userId);

        Page<AuditLog> result =
                toPage(auditLogs, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(result)
        );
    }

    /**
     * Get audit logs for a specific entity.
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize
        );

        List<AuditLog> auditLogs =
                auditLogRepository.findByEntityTypeAndEntityId(
                        entityType,
                        entityId
                );

        Page<AuditLog> result =
                toPage(auditLogs, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(result)
        );
    }

    /**
     * Manually log an audit event.
     *
     * This uses the existing AuditLogService overload:
     *
     * logAuditEvent(
     *     UUID,
     *     String,
     *     String,
     *     UUID,
     *     String,
     *     HttpServletRequest,
     *     boolean,
     *     String,
     *     String
     * )
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> logAuditEvent(
            @RequestParam UUID userId,
            @RequestParam String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam String description,
            @RequestParam(required = false) String metadata,
            HttpServletRequest request) {

        auditLogService.logAuditEvent(
                userId,
                actionType,
                entityType,
                entityId,
                description,
                request,
                true,
                null,
                metadata
        );

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    /**
     * Convert an existing List into a Spring Data Page.
     *
     * This is required because the current repository methods
     * for user/entity lookups return List rather than Page.
     */
    private Page<AuditLog> toPage(
            List<AuditLog> logs,
            Pageable pageable) {

        if (logs == null || logs.isEmpty()) {
            return new PageImpl<>(
                    List.of(),
                    pageable,
                    0
            );
        }

        int totalElements = logs.size();

        int start =
                (int) pageable.getOffset();

        if (start >= totalElements) {
            return new PageImpl<>(
                    List.of(),
                    pageable,
                    totalElements
            );
        }

        int end =
                Math.min(
                        start + pageable.getPageSize(),
                        totalElements
                );

        List<AuditLog> pageContent =
                logs.subList(start, end);

        return new PageImpl<>(
                pageContent,
                pageable,
                totalElements
        );
    }
}
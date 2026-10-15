package com.platform.common.service;

import com.platform.common.entity.AuditLog;
import com.platform.common.repository.AuditLogRepository;
import com.platform.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for handling audit logging (Feature 38: Security & Audit Logs)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an audit event
     */
    @Transactional
    public void logAuditEvent(
            UUID userId,
            String actionType,
            String entityType,
            UUID entityId,
            String description,
            HttpServletRequest request,
            boolean success,
            String errorMessage,
            String metadata) {

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(request != null ? IpUtil.getClientIpAddress(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .success(success)
                .errorMessage(errorMessage)
                .metadata(metadata)
                .timestamp(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log recorded: {} - {}", actionType, description);
    }

    /**
     * Log a successful audit event
     */
    @Transactional
    public void logAuditEvent(
            UUID userId,
            String actionType,
            String entityType,
            UUID entityId,
            String description,
            HttpServletRequest request,
            String metadata) {

        logAuditEvent(userId, actionType, entityType, entityId, description, request, true, null, metadata);
    }

    /**
     * Log a failed audit event
     */
    @Transactional
    public void logAuditEvent(
            UUID userId,
            String actionType,
            String entityType,
            UUID entityId,
            String description,
            HttpServletRequest request,
            String errorMessage,
            String metadata) {

        logAuditEvent(userId, actionType, entityType, entityId, description, request, false, errorMessage, metadata);
    }

    /**
     * Log an audit event without HTTP request context
     */
    @Transactional
    public void logAuditEvent(
            UUID userId,
            String actionType,
            String entityType,
            UUID entityId,
            String description,
            boolean success,
            String errorMessage,
            String metadata) {

        logAuditEvent(userId, actionType, entityType, entityId, description, null, success, errorMessage, metadata);
    }
}
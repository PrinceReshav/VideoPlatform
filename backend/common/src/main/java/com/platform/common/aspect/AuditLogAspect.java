package com.platform.common.aspect;

import com.platform.common.service.AuditLogService;
import com.platform.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;

/**
 * Aspect for automatic audit logging of service methods (Feature 38: Security & Audit Logs)
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    /**
     * Get current request if available
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get current user ID if available
     */
    private UUID getCurrentUserId() {
        // In a real implementation, this would extract the user ID from security context
        // For now, we'll return null and let the calling code specify it when needed
        return null;
    }

    /**
     * Log before method execution
     */
    @Before("execution(* com.platform.*.service.*.*(..)) && !execution(* com.platform.*.service.*.*Audit*(..))")
    public void logBefore(JoinPoint joinPoint) {
        // We don't log before by default to avoid double logging
        // We'll log after successful execution or on exception
    }

    /**
     * Log after successful method execution
     */
    @AfterReturning(pointcut = "execution(* com.platform.*.service.*.*(..)) && !execution(* com.platform.*.service.*.*Audit*(..))", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            // Only log certain sensitive operations
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            // Log specific security-relevant operations
            if (isSecurityRelevantOperation(methodName)) {
                HttpServletRequest request = getCurrentRequest();
                UUID userId = getCurrentUserId(); // Would be extracted from auth context in real implementation

                String description = String.format("Executed %s.%s", className, methodName);
                String methodArgs = Arrays.toString(joinPoint.getArgs());

                auditLogService.logAuditEvent(
                    userId,
                    "EXECUTE_" + methodName.toUpperCase(),
                    className,
                    null, // entityId would need to be extracted from args in real implementation
                    description + " Args: " + methodArgs,
                    request,
                    "{}"
                );
            }
        } catch (Exception e) {
            log.error("Failed to log audit event after method execution: {}", e.getMessage());
        }
    }

    /**
     * Log after method throws exception
     */
    @AfterThrowing(pointcut = "execution(* com.platform.*.service.*.*(..)) && !execution(* com.platform.*.service.*.*Audit*(..))", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Exception exception) {
        try {
            // Log exceptions for security-relevant operations
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            // Log specific security-relevant operations that failed
            if (isSecurityRelevantOperation(methodName)) {
                HttpServletRequest request = getCurrentRequest();
                UUID userId = getCurrentUserId(); // Would be extracted from auth context in real implementation

                String description = String.format("Failed to execute %s.%s", className, methodName);
                String methodArgs = Arrays.toString(joinPoint.getArgs());

                auditLogService.logAuditEvent(
                    userId,
                    "EXECUTE_FAILED_" + methodName.toUpperCase(),
                    className,
                    null, // entityId would need to be extracted from args in real implementation
                    description + " Args: " + methodArgs,
                    request,
                    exception.getMessage(),
                    "{}"
                );
            }
        } catch (Exception e) {
            log.error("Failed to log audit event after method exception: {}", e.getMessage());
        }
    }

    /**
     * Determine if a method name represents a security-relevant operation
     */
    private boolean isSecurityRelevantOperation(String methodName) {
        // List of method names that are considered security-relevant for auditing
        return switch (methodName.toLowerCase()) {
            case "createvideo", "updatevideo", "deletevideo", "publishvideo", "unpublishvideo",
                 "login", "logout", "register", "changepassword", "resetpassword",
                 "grantrole", "revokerole", "blockuser", "unblockuser",
                 "updatechannel", "deletechannel", "updatecreatorprofile",
                 "updatesubscription", "updatenotificationpreferences" ->
                true;
            default -> false;
        };
    }
}
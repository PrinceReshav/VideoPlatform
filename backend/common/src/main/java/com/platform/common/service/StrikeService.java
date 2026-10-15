package com.platform.common.service;

import com.platform.common.entity.Notification;
import com.platform.common.entity.Report;
import com.platform.common.entity.Strike;
import com.platform.common.dto.StrikeResponse;
import com.platform.common.repository.NotificationRepository;
import com.platform.common.repository.ReportRepository;
import com.platform.common.repository.StrikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing strikes.
 *
 * Feature 18: Reporting / Strikes / Trust System
 *
 * A strike represents a policy violation assigned to a USER,
 * CHANNEL, or CREATOR_PROFILE.
 */
@Service
@RequiredArgsConstructor
public class StrikeService {

    private static final int MAX_ACTIVE_STRIKES = 3;
    private static final long STRIKE_EXPIRY_DAYS = 30L;

    private final StrikeRepository strikeRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Create a new strike.
     *
     * The strike is persisted and a notification is generated
     * for the target.
     */
    @Transactional
    public Strike createStrike(Strike strike) {

        if (strike == null) {
            throw new IllegalArgumentException(
                    "Strike must not be null"
            );
        }

        if (strike.getTargetId() == null) {
            throw new IllegalArgumentException(
                    "Strike target ID must not be null"
            );
        }

        if (strike.getTargetType() == null
                || strike.getTargetType().isBlank()) {

            throw new IllegalArgumentException(
                    "Strike target type must not be null or blank"
            );
        }

        if (strike.getReason() == null
                || strike.getReason().isBlank()) {

            throw new IllegalArgumentException(
                    "Strike reason must not be null or blank"
            );
        }

        /*
         * Set issued time when the caller has not already supplied one.
         */
        if (strike.getIssuedAt() == null) {
            strike.setIssuedAt(Instant.now());
        }

        /*
         * New strikes are active by default.
         */
        strike.setActive(true);

        Strike savedStrike =
                strikeRepository.save(strike);

        createStrikeNotification(savedStrike);

        return savedStrike;
    }

    /**
     * Get a strike by ID.
     */
    @Transactional(readOnly = true)
    public Strike getStrikeById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Strike ID must not be null"
            );
        }

        return strikeRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Strike not found with id: " + id
                        )
                );
    }

    /**
     * Get strikes by target ID and target type.
     */
    @Transactional(readOnly = true)
    public List<Strike> getStrikesByTargetIdAndTargetType(
            UUID targetId,
            String targetType) {

        validateTarget(targetId, targetType);

        return strikeRepository.findByTargetIdAndTargetType(
                targetId,
                targetType
        );
    }

    /**
     * Get currently active strikes by target ID and target type.
     */
    @Transactional(readOnly = true)
    public List<Strike> getActiveStrikesByTargetIdAndTargetType(
            UUID targetId,
            String targetType) {

        validateTarget(targetId, targetType);

        return strikeRepository
                .findByTargetIdAndTargetTypeAndIsActiveTrue(
                        targetId,
                        targetType
                );
    }

    /**
     * Get strikes by reason.
     */
    @Transactional(readOnly = true)
    public List<Strike> getStrikesByReason(
            String reason) {

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Strike reason must not be null or blank"
            );
        }

        return strikeRepository.findByReason(reason);
    }

    /**
     * Get strikes issued within a timestamp range.
     *
     * IMPORTANT:
     * Strike uses issuedAt as its timestamp field.
     * It does not currently have createdAt.
     */
    @Transactional(readOnly = true)
    public List<Strike> getStrikesByTimestampRange(
            Instant startTime,
            Instant endTime) {

        validateTimeRange(startTime, endTime);

        return strikeRepository.findByIssuedAtBetween(
                startTime,
                endTime
        );
    }

    /**
     * Get strikes with multiple filters.
     *
     * The current repository supports a Pageable argument.
     * Pageable.unpaged() preserves the existing List-based service API
     * while allowing the repository query to execute without pagination.
     */
    @Transactional(readOnly = true)
    public List<Strike> getStrikesByCriteria(
            UUID targetId,
            String targetType,
            String reason,
            Boolean isActive,
            UUID issuedBy,
            UUID reportId,
            Instant startTime,
            Instant endTime) {

        validateOptionalTimeRange(startTime, endTime);

        return strikeRepository.findByCriteria(
                        targetId,
                        targetType,
                        reason,
                        isActive,
                        issuedBy,
                        reportId,
                        startTime,
                        endTime,
                        Pageable.unpaged()
                )
                .getContent();
    }

    /**
     * Count active strikes for a target.
     *
     * The current repository does not expose a COUNT query,
     * so this uses the existing active-strike query.
     */
    @Transactional(readOnly = true)
    public long countActiveStrikesByTargetIdAndTargetType(
            UUID targetId,
            String targetType) {

        validateTarget(targetId, targetType);

        return strikeRepository
                .findByTargetIdAndTargetTypeAndIsActiveTrue(
                        targetId,
                        targetType
                )
                .size();
    }

    /**
     * Automatically create a strike when a report has been approved.
     *
     * Current policy:
     *
     * - Only APPROVED reports create strikes.
     * - A report cannot assign a second strike once it already
     *   has strikesAssigned > 0.
     * - Maximum active strikes per target = 3.
     * - Each automatically generated strike expires after 30 days.
     */
    @Transactional
    public void createStrikeFromApprovedReport(
            UUID reportId) {

        if (reportId == null) {
            throw new IllegalArgumentException(
                    "Report ID must not be null"
            );
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Report not found with id: " + reportId
                        )
                );

        /*
         * Only approved reports are eligible.
         */
        if (!"APPROVED".equalsIgnoreCase(
                report.getStatus())) {

            return;
        }

        /*
         * A report that already has an assigned strike must
         * not generate another one.
         */
        int assignedStrikes =
                report.getStrikesAssigned() == null
                        ? 0
                        : report.getStrikesAssigned();

        if (assignedStrikes > 0) {
            return;
        }

        /*
         * Determine how many active strikes already exist
         * for the target.
         */
        long existingStrikes =
                countActiveStrikesByTargetIdAndTargetType(
                        report.getTargetId(),
                        report.getTargetType()
                );

        int remainingStrikeCapacity =
                Math.max(
                        0,
                        MAX_ACTIVE_STRIKES
                                - (int) existingStrikes
                );

        /*
         * One approved report generates at most one strike.
         */
        int strikesToAssign =
                Math.min(
                        1,
                        remainingStrikeCapacity
                );

        if (strikesToAssign <= 0) {
            return;
        }

        Instant now = Instant.now();

        Strike strike = Strike.builder()
                .targetId(report.getTargetId())
                .targetType(report.getTargetType())
                .reason(report.getReason())
                .details(
                        "Strike assigned from approved report: "
                                + report.getDescription()
                )
                .issuedAt(now)
                .expiresAt(
                        now.plusSeconds(
                                STRIKE_EXPIRY_DAYS * 24L * 60L * 60L
                        )
                )
                .issuedBy(report.getReviewedBy())
                .reportId(report.getId())
                .isActive(true)
                .build();

        Strike savedStrike =
                createStrike(strike);

        /*
         * Record the strike against the report.
         */
        report.setStrikesAssigned(
                (int) existingStrikes
                        + strikesToAssign
        );

        report.setActionTaken("STRIKE");

        reportRepository.save(report);
    }

    /**
     * Create a notification for a newly issued strike.
     *
     * IMPORTANT:
     *
     * For USER targets, targetId is the receiving user ID.
     *
     * For CHANNEL and CREATOR_PROFILE targets, targetId is NOT
     * necessarily a user ID. Therefore this implementation only
     * directly assigns the notification to USER targets.
     *
     * A future production enhancement should resolve the owner
     * of CHANNEL / CREATOR_PROFILE before creating a notification.
     */
    private void createStrikeNotification(
            Strike strike) {

        String targetType =
                strike.getTargetType() == null
                        ? ""
                        : strike.getTargetType()
                        .trim()
                        .toUpperCase();

        String title;
        String message;

        switch (targetType) {

            case "USER":
                title = "Account Strike Issued";

                message =
                        "Your account has received a strike for "
                                + safeText(strike.getReason())
                                + ". Accumulating strikes may lead "
                                + "to account restrictions.";
                break;

            case "CHANNEL":
                title = "Channel Strike Issued";

                message =
                        "Your channel has received a strike for "
                                + safeText(strike.getReason())
                                + ". Accumulating strikes may lead "
                                + "to channel restrictions.";
                break;

            case "CREATOR_PROFILE":
                title = "Creator Profile Strike Issued";

                message =
                        "Your creator profile has received a strike for "
                                + safeText(strike.getReason())
                                + ". Accumulating strikes may lead "
                                + "to profile restrictions.";
                break;

            default:
                title = "Strike Issued";

                message =
                        "You have received a strike for "
                                + safeText(strike.getReason())
                                + ". Accumulating strikes may lead "
                                + "to restrictions.";
                break;
        }

        /*
         * The current Notification entity requires a userId.
         *
         * A channel ID or creator-profile ID must not blindly be
         * treated as a user ID.
         *
         * Therefore only USER strikes can safely create the
         * notification with the current entity model.
         */
        if (!"USER".equals(targetType)) {
            return;
        }

        Notification notification =
                Notification.builder()
                        .userId(strike.getTargetId())
                        .notificationType("STRIKE_ISSUED")
                        .title(title)
                        .message(message)
                        .entityId(strike.getId())
                        .entityType("STRIKE")
                        .data(
                                buildNotificationData(strike)
                        )
                        .createdAt(Instant.now())
                        .isRead(false)
                        .isArchived(false)
                        .deleted(false)
                        .build();

        notificationRepository.save(notification);
    }

    /**
     * Convert Strike entity to API response DTO.
     *
     * Strike currently has no createdAt/updatedAt fields.
     * Therefore those response properties are intentionally not
     * populated here.
     *
     * The DTO can remain backward compatible and will simply
     * return null for those two properties until the entity/schema
     * is explicitly enhanced.
     */
    public StrikeResponse mapToResponse(
            Strike strike) {

        if (strike == null) {
            return null;
        }

        return StrikeResponse.builder()
                .id(strike.getId())
                .targetId(strike.getTargetId())
                .targetType(strike.getTargetType())
                .reason(strike.getReason())
                .details(strike.getDetails())
                .isActive(strike.isActive())
                .issuedAt(strike.getIssuedAt())
                .expiresAt(strike.getExpiresAt())
                .issuedBy(strike.getIssuedBy())
                .reportId(strike.getReportId())
                .build();
    }

    /**
     * Convert a list of Strike entities to response DTOs.
     */
    public List<StrikeResponse> mapToResponseList(
            List<Strike> strikes) {

        if (strikes == null || strikes.isEmpty()) {
            return List.of();
        }

        return strikes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validate target information.
     */
    private void validateTarget(
            UUID targetId,
            String targetType) {

        if (targetId == null) {
            throw new IllegalArgumentException(
                    "Target ID must not be null"
            );
        }

        if (targetType == null
                || targetType.isBlank()) {

            throw new IllegalArgumentException(
                    "Target type must not be null or blank"
            );
        }
    }

    /**
     * Validate a required timestamp range.
     */
    private void validateTimeRange(
            Instant startTime,
            Instant endTime) {

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(
                    "Start time must not be after end time"
            );
        }
    }

    /**
     * Validate an optional timestamp range.
     */
    private void validateOptionalTimeRange(
            Instant startTime,
            Instant endTime) {

        if (startTime != null
                && endTime != null
                && startTime.isAfter(endTime)) {

            throw new IllegalArgumentException(
                    "Start time must not be after end time"
            );
        }
    }

    /**
     * Safely convert nullable text into a notification-safe value.
     */
    private String safeText(String value) {

        if (value == null || value.isBlank()) {
            return "a policy violation";
        }

        return value;
    }

    /**
     * Build notification JSON without introducing another JSON
     * dependency into common.
     *
     * The values are escaped sufficiently for the current simple
     * notification payload.
     */
    private String buildNotificationData(
            Strike strike) {

        return "{"
                + "\"strikeId\":\""
                + escapeJson(
                String.valueOf(strike.getId())
        )
                + "\","
                + "\"reason\":\""
                + escapeJson(
                safeText(strike.getReason())
        )
                + "\""
                + "}";
    }

    /**
     * Basic JSON string escaping.
     */
    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
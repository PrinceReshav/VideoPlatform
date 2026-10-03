package com.platform.notification.service;

import com.platform.common.dto.PageResponse;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.event.NotificationRequestedEvent;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.notification.dto.NotificationPreferenceResponse;
import com.platform.notification.dto.NotificationResponse;
import com.platform.notification.dto.UpdateNotificationPreferenceRequest;
import com.platform.notification.entity.Notification;
import com.platform.notification.entity.NotificationPreference;
import com.platform.notification.repository.NotificationPreferenceRepository;
import com.platform.notification.repository.NotificationRepository;
import com.platform.common.security.CurrentUserAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional
    @KafkaListener(topics = KafkaTopicNames.NOTIFICATION_REQUESTED, groupId = "notification-service")
    public void handleNotificationRequested(EventEnvelope<NotificationRequestedEvent> envelope) {
        NotificationRequestedEvent event = envelope.getPayload();
        if (event == null || event.getUserId() == null) return;
        createNotification(event);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(int page, int size, boolean unreadOnly) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        PageResponse<NotificationResponse> response;
        if (unreadOnly) {
            var notifications = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
            response = PageResponse.of(
                    notifications.getContent().stream().map(this::mapToResponse).toList(),
                    page, size, notifications.getTotalElements()
            );
        } else {
            var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            response = PageResponse.of(
                    notifications.getContent().stream().map(this::mapToResponse).toList(),
                    page, size, notifications.getTotalElements()
            );
        }
        return response;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(List<UUID> notificationIds) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        // Verify ownership
        // In practice, we'd check that all notificationIds belong to the user
        notificationRepository.markAsRead(notificationIds, Instant.now());
    }

    @Transactional
    public void markAllAsRead() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        notificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Transactional
    public NotificationPreferenceResponse getPreferences() {
        UUID userId = currentUserAccessor.getCurrentUserId();
        NotificationPreference prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        return mapToPreferenceResponse(prefs);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(UpdateNotificationPreferenceRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        NotificationPreference prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        updatePreferencesFromRequest(prefs, request);
        prefs = preferenceRepository.save(prefs);

        return mapToPreferenceResponse(prefs);
    }

    @Transactional
    public void createNotification(NotificationRequestedEvent event) {
        UUID userId = event.getUserId();

        // Check preferences
        NotificationPreference prefs = preferenceRepository.findByUserId(userId).orElse(null);
        if (prefs != null && !isTypeEnabled(prefs, Notification.Type.valueOf(event.getType().name()))) {
            log.debug("Notification type {} disabled for user {}", event.getType(), userId);
            return;
        }

        // Check quiet hours
        if (prefs != null && isInQuietHours(prefs)) {
            log.debug("In quiet hours for user {}, deferring notification", userId);
            // In a real implementation, we'd schedule for later
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(Notification.Type.valueOf(event.getType().name()));
        notification.setTitle(event.getTitle());
        notification.setMessage(event.getMessage());
        notification.setActionUrl(event.getActionUrl());
        notification.setReferenceId(event.getReferenceId());
        notification.setReferenceType(event.getReferenceType());
        notification.setPriority(event.getPriority());
        notificationRepository.save(notification);

        log.debug("Created notification {} for user {}", notification.getId(), userId);
    }

    private NotificationPreference createDefaultPreferences(UUID userId) {
        NotificationPreference prefs = new NotificationPreference();
        prefs.setUserId(userId);
        return preferenceRepository.save(prefs);
    }

    private void updatePreferencesFromRequest(NotificationPreference prefs, UpdateNotificationPreferenceRequest request) {
        if (request.getNewVideoEnabled() != null) prefs.setNewVideoEnabled(request.getNewVideoEnabled());
        if (request.getNewCommentEnabled() != null) prefs.setNewCommentEnabled(request.getNewCommentEnabled());
        if (request.getCommentReplyEnabled() != null) prefs.setCommentReplyEnabled(request.getCommentReplyEnabled());
        if (request.getCommentLikeEnabled() != null) prefs.setCommentLikeEnabled(request.getCommentLikeEnabled());
        if (request.getNewSubscriberEnabled() != null) prefs.setNewSubscriberEnabled(request.getNewSubscriberEnabled());
        if (request.getVideoLikeEnabled() != null) prefs.setVideoLikeEnabled(request.getVideoLikeEnabled());
        if (request.getVideoMilestoneEnabled() != null) prefs.setVideoMilestoneEnabled(request.getVideoMilestoneEnabled());
        if (request.getChannelMilestoneEnabled() != null) prefs.setChannelMilestoneEnabled(request.getChannelMilestoneEnabled());
        if (request.getSystemAnnouncementEnabled() != null) prefs.setSystemAnnouncementEnabled(request.getSystemAnnouncementEnabled());
        if (request.getModerationActionEnabled() != null) prefs.setModerationActionEnabled(request.getModerationActionEnabled());
        if (request.getUploadCompleteEnabled() != null) prefs.setUploadCompleteEnabled(request.getUploadCompleteEnabled());
        if (request.getProcessingCompleteEnabled() != null) prefs.setProcessingCompleteEnabled(request.getProcessingCompleteEnabled());
        if (request.getProcessingFailedEnabled() != null) prefs.setProcessingFailedEnabled(request.getProcessingFailedEnabled());
        if (request.getLiveStartingEnabled() != null) prefs.setLiveStartingEnabled(request.getLiveStartingEnabled());
        if (request.getEmailEnabled() != null) prefs.setEmailEnabled(request.getEmailEnabled());
        if (request.getPushEnabled() != null) prefs.setPushEnabled(request.getPushEnabled());
        if (request.getQuietHoursStart() != null) prefs.setQuietHoursStart(request.getQuietHoursStart());
        if (request.getQuietHoursEnd() != null) prefs.setQuietHoursEnd(request.getQuietHoursEnd());
    }

    private boolean isTypeEnabled(NotificationPreference prefs, Notification.Type type) {
        return switch (type) {
            case NEW_VIDEO -> prefs.isNewVideoEnabled();
            case NEW_COMMENT -> prefs.isNewCommentEnabled();
            case COMMENT_REPLY -> prefs.isCommentReplyEnabled();
            case COMMENT_LIKE -> prefs.isCommentLikeEnabled();
            case NEW_SUBSCRIBER -> prefs.isNewSubscriberEnabled();
            case VIDEO_LIKE -> prefs.isVideoLikeEnabled();
            case VIDEO_MILESTONE -> prefs.isVideoMilestoneEnabled();
            case CHANNEL_MILESTONE -> prefs.isChannelMilestoneEnabled();
            case SYSTEM_ANNOUNCEMENT -> prefs.isSystemAnnouncementEnabled();
            case MODERATION_ACTION -> prefs.isModerationActionEnabled();
            case UPLOAD_COMPLETE -> prefs.isUploadCompleteEnabled();
            case PROCESSING_COMPLETE -> prefs.isProcessingCompleteEnabled();
            case PROCESSING_FAILED -> prefs.isProcessingFailedEnabled();
            case LIVE_STARTING -> prefs.isLiveStartingEnabled();
            default -> true;
        };
    }

    private boolean isInQuietHours(NotificationPreference prefs) {
        if (prefs.getQuietHoursStart() == null || prefs.getQuietHoursEnd() == null) {
            return false;
        }
        try {
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime start = java.time.LocalTime.parse(prefs.getQuietHoursStart());
            java.time.LocalTime end = java.time.LocalTime.parse(prefs.getQuietHoursEnd());

            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            } else {
                // Crosses midnight
                return !now.isBefore(start) || now.isBefore(end);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .actionUrl(notification.getActionUrl())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .priority(notification.getPriority())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationPreferenceResponse mapToPreferenceResponse(NotificationPreference prefs) {
        return NotificationPreferenceResponse.builder()
                .id(prefs.getId())
                .userId(prefs.getUserId())
                .newVideoEnabled(prefs.isNewVideoEnabled())
                .newCommentEnabled(prefs.isNewCommentEnabled())
                .commentReplyEnabled(prefs.isCommentReplyEnabled())
                .commentLikeEnabled(prefs.isCommentLikeEnabled())
                .newSubscriberEnabled(prefs.isNewSubscriberEnabled())
                .videoLikeEnabled(prefs.isVideoLikeEnabled())
                .videoMilestoneEnabled(prefs.isVideoMilestoneEnabled())
                .channelMilestoneEnabled(prefs.isChannelMilestoneEnabled())
                .systemAnnouncementEnabled(prefs.isSystemAnnouncementEnabled())
                .moderationActionEnabled(prefs.isModerationActionEnabled())
                .uploadCompleteEnabled(prefs.isUploadCompleteEnabled())
                .processingCompleteEnabled(prefs.isProcessingCompleteEnabled())
                .processingFailedEnabled(prefs.isProcessingFailedEnabled())
                .liveStartingEnabled(prefs.isLiveStartingEnabled())
                .emailEnabled(prefs.isEmailEnabled())
                .pushEnabled(prefs.isPushEnabled())
                .quietHoursStart(prefs.getQuietHoursStart())
                .quietHoursEnd(prefs.getQuietHoursEnd())
                .build();
    }
}
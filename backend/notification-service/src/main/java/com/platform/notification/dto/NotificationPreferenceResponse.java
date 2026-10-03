package com.platform.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {
    private UUID id;
    private UUID userId;
    private boolean newVideoEnabled;
    private boolean newCommentEnabled;
    private boolean commentReplyEnabled;
    private boolean commentLikeEnabled;
    private boolean newSubscriberEnabled;
    private boolean videoLikeEnabled;
    private boolean videoMilestoneEnabled;
    private boolean channelMilestoneEnabled;
    private boolean systemAnnouncementEnabled;
    private boolean moderationActionEnabled;
    private boolean uploadCompleteEnabled;
    private boolean processingCompleteEnabled;
    private boolean processingFailedEnabled;
    private boolean liveStartingEnabled;
    private boolean emailEnabled;
    private boolean pushEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
}
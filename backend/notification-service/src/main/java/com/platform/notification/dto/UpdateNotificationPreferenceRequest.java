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
public class UpdateNotificationPreferenceRequest {
    private Boolean newVideoEnabled;
    private Boolean newCommentEnabled;
    private Boolean commentReplyEnabled;
    private Boolean commentLikeEnabled;
    private Boolean newSubscriberEnabled;
    private Boolean videoLikeEnabled;
    private Boolean videoMilestoneEnabled;
    private Boolean channelMilestoneEnabled;
    private Boolean systemAnnouncementEnabled;
    private Boolean moderationActionEnabled;
    private Boolean uploadCompleteEnabled;
    private Boolean processingCompleteEnabled;
    private Boolean processingFailedEnabled;
    private Boolean liveStartingEnabled;
    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
}
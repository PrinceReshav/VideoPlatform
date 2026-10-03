package com.platform.notification.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notification_preferences", schema = "notification_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_preferences_user", columnNames = "user_id")
        })
@Getter
@Setter
public class NotificationPreference extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "new_video_enabled", nullable = false)
    private boolean newVideoEnabled = true;

    @Column(name = "new_comment_enabled", nullable = false)
    private boolean newCommentEnabled = true;

    @Column(name = "comment_reply_enabled", nullable = false)
    private boolean commentReplyEnabled = true;

    @Column(name = "comment_like_enabled", nullable = false)
    private boolean commentLikeEnabled = true;

    @Column(name = "new_subscriber_enabled", nullable = false)
    private boolean newSubscriberEnabled = true;

    @Column(name = "video_like_enabled", nullable = false)
    private boolean videoLikeEnabled = true;

    @Column(name = "video_milestone_enabled", nullable = false)
    private boolean videoMilestoneEnabled = true;

    @Column(name = "channel_milestone_enabled", nullable = false)
    private boolean channelMilestoneEnabled = true;

    @Column(name = "system_announcement_enabled", nullable = false)
    private boolean systemAnnouncementEnabled = true;

    @Column(name = "moderation_action_enabled", nullable = false)
    private boolean moderationActionEnabled = true;

    @Column(name = "upload_complete_enabled", nullable = false)
    private boolean uploadCompleteEnabled = true;

    @Column(name = "processing_complete_enabled", nullable = false)
    private boolean processingCompleteEnabled = true;

    @Column(name = "processing_failed_enabled", nullable = false)
    private boolean processingFailedEnabled = true;

    @Column(name = "live_starting_enabled", nullable = false)
    private boolean liveStartingEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = false;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = false;

    @Column(name = "quiet_hours_start", length = 5)
    private String quietHoursStart; // HH:mm format

    @Column(name = "quiet_hours_end", length = 5)
    private String quietHoursEnd;
}
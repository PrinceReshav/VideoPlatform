package com.platform.common.kafka;

public final class KafkaTopicNames {

    // Video topics
    public static final String VIDEO_UPLOADED = "video.uploaded.v1";
    public static final String VIDEO_PROCESSING_STARTED = "video.processing.started.v1";
    public static final String VIDEO_PROCESSING_PROGRESS = "video.processing.progress.v1";
    public static final String VIDEO_PROCESSING_COMPLETED = "video.processing.completed.v1";
    public static final String VIDEO_PROCESSING_FAILED = "video.processing.failed.v1";
    public static final String VIDEO_PUBLISHED = "video.published.v1";
    public static final String VIDEO_UNPUBLISHED = "video.unpublished.v1";
    public static final String VIDEO_DELETED = "video.deleted.v1";

    // Engagement topics
    public static final String VIDEO_VIEWED = "video.viewed.v1";
    public static final String VIDEO_LIKED = "video.liked.v1";
    public static final String COMMENT_CREATED = "comment.created.v1";

    // Subscription topics
    public static final String SUBSCRIPTION_CREATED = "subscription.created.v1";
    public static final String SUBSCRIPTION_DELETED = "subscription.deleted.v1";

    // Notification topics
    public static final String NOTIFICATION_REQUESTED = "notification.requested.v1";
    public static final String MODERATION_ACTION_APPLIED = "moderation.action.applied.v1";

    // Dead Letter Queues
    public static final String VIDEO_UPLOADED_DLQ = "video.uploaded.v1.dlq";
    public static final String VIDEO_PROCESSING_COMPLETED_DLQ = "video.processing.completed.v1.dlq";
    public static final String VIDEO_PUBLISHED_DLQ = "video.published.v1.dlq";

    // Consumer Groups
    public static final String CONSUMER_GROUP_VIDEO_SERVICE = "video-service";
    public static final String CONSUMER_GROUP_TRANSCODING_SERVICE = "transcoding-service";
    public static final String CONSUMER_GROUP_SEARCH_SERVICE = "search-service";
    public static final String CONSUMER_GROUP_ANALYTICS_SERVICE = "analytics-service";
    public static final String CONSUMER_GROUP_NOTIFICATION_SERVICE = "notification-service";
    public static final String CONSUMER_GROUP_ENGAGEMENT_SERVICE = "engagement-service";
    public static final String CONSUMER_GROUP_SUBSCRIPTION_SERVICE = "subscription-service";

    private KafkaTopicNames() {}
}
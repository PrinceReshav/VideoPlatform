package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for subscription responses (Feature 16: Subscribe/Follow System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    /** The unique identifier of the subscription */
    private UUID id;

    /** The user who is subscribing/following */
    private UUID subscriberId;

    /** The target being subscribed to (channel ID or creator profile ID) */
    private UUID targetId;

    /** The type of target being subscribed to */
    private String targetType; // CHANNEL, CREATOR_PROFILE

    /** Whether this is a subscription (to channel) or follow (to creator) */
    private String subscriptionType; // SUBSCRIPTION, FOLLOW

    /** Whether the subscription is active */
    private boolean active;

    /** Notification preferences for this subscription */
    private String notificationPreference; // ALL, PERSONAL_UPDATES, NONE

    /** Whether to show this subscription publicly on the subscriber's profile */
    private boolean publicSubscription;

    /** Timestamp when the subscription was created */
    private Instant createdAt;

    /** Timestamp when the subscription was last updated */
    private Instant updatedAt;

    /** Timestamp when the subscription ended (if applicable) */
    private Instant endedAt;
}
package com.platform.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for subscription requests (Feature 16: Subscribe/Follow System)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    /** The user who is subscribing/following */
    @NotNull
    private UUID subscriberId;

    /** The target being subscribed to (channel ID or creator profile ID) */
    @NotNull
    private UUID targetId;

    /** The type of target being subscribed to */
    @NotBlank
    @Size(max = 20)
    private String targetType; // CHANNEL, CREATOR_PROFILE

    /** Whether this is a subscription (to channel) or follow (to creator) */
    @NotBlank
    @Size(max = 20)
    private String subscriptionType; // SUBSCRIPTION, FOLLOW

    /** Notification preferences for this subscription */
    @Size(max = 50)
    private String notificationPreference; // ALL, PERSONAL_UPDATES, NONE

    /** Whether to show this subscription publicly on the subscriber's profile */
    private boolean publicSubscription = true;

    /** Timestamp when the subscription should end (if applicable) */
    private Instant endsAt;
}
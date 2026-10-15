package com.platform.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscription entity for Feature 16: Subscribe/Follow System
 * Represents a user subscribing to a channel or following a creator
 */
@Entity
@Table(name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_subscriptions_subscriber_target", columnNames = "subscriber_id, target_id, target_type")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE subscriptions SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Subscription {

    @Id
    @GeneratedValue
    private UUID id;

    /** The user who is subscribing/following */
    @Column(nullable = false)
    private UUID subscriberId;

    /** The target being subscribed to (channel ID or creator profile ID) */
    @Column(nullable = false)
    private UUID targetId;

    /** The type of target being subscribed to */
    @Column(length = 20, nullable = false)
    private String targetType; // CHANNEL, CREATOR_PROFILE

    /** Whether this is a subscription (to channel) or follow (to creator) */
    @Column(length = 20, nullable = false)
    private String subscriptionType; // SUBSCRIPTION, FOLLOW

    /** Whether the subscription is active */
    @Column(nullable = false)
    private boolean active = true;

    /** Notification preferences for this subscription */
    @Column(length = 50)
    private String notificationPreference; // ALL, PERSONAL_UPDATES, NONE

    /** Whether to show this subscription publicly on the subscriber's profile */
    private boolean publicSubscription = true;

    /** Timestamp when the subscription was created */
    private Instant createdAt;

    /** Timestamp when the subscription was last updated */
    private Instant updatedAt;

    /** Timestamp when the subscription ended (if applicable) */
    private Instant endedAt;

    /** Soft delete flag */
    private boolean deleted = false;
}
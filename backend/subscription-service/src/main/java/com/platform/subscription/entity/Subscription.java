package com.platform.subscription.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", schema = "subscription_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_subscriptions_user_channel", columnNames = {"user_id", "channel_id"})
        },
        indexes = {
                @Index(name = "idx_subscriptions_user", columnList = "user_id"),
                @Index(name = "idx_subscriptions_channel", columnList = "channel_id"),
                @Index(name = "idx_subscriptions_created_at", columnList = "created_at"),
                @Index(name = "idx_subscriptions_notifications", columnList = "notifications_enabled")
        })
@Getter
@Setter
public class Subscription extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
package com.platform.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private UUID id;
    private UUID userId;
    private UUID channelId;
    private String channelName;
    private String channelSlug;
    private String channelAvatarUrl;
    private boolean notificationsEnabled;
    private Instant createdAt;
}
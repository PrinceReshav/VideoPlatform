package com.platform.subscription.event;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class SubscriptionCreatedEvent {
    private UUID subscriptionId;
    private UUID userId;
    private UUID channelId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}
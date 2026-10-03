package com.platform.common.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope<T> {
    private String eventId;
    private String eventType;
    private String version;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    private String correlationId;
    private String causationId;
    private String sourceService;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
    private T payload;
    private Map<String, String> headers;
    private int retryCount;

    public static <T> EventEnvelope<T> create(String eventType, String version, T payload, String sourceService) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .version(version)
                .timestamp(Instant.now())
                .correlationId(UUID.randomUUID().toString())
                .sourceService(sourceService)
                .payload(payload)
                .retryCount(0)
                .build();
    }

    public static <T> EventEnvelope<T> createWithCorrelation(String eventType, String version,
                                                              T payload, String sourceService,
                                                              String correlationId, String causationId) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .version(version)
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .causationId(causationId)
                .sourceService(sourceService)
                .payload(payload)
                .retryCount(0)
                .build();
    }

    public EventEnvelope<T> withRetry() {
        return EventEnvelope.<T>builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .version(this.version)
                .timestamp(this.timestamp)
                .correlationId(this.correlationId)
                .causationId(this.causationId)
                .sourceService(this.sourceService)
                .payload(this.payload)
                .headers(this.headers)
                .retryCount(this.retryCount + 1)
                .build();
    }
}
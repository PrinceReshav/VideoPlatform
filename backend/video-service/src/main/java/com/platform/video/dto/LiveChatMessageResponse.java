package com.platform.video.dto;

import com.platform.video.entity.LiveChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for live chat message responses (Feature 31: Live Streaming Platform)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatMessageResponse {

    /** The chat message ID */
    private UUID id;

    /** The live stream this message belongs to */
    private UUID liveStreamId;

    /** The user who sent this message */
    private UUID senderId;

    /** The message content */
    private String message;

    /** Whether this is a system message (e.g., user joined/left) */
    private boolean isSystemMessage;

    /** Whether this message has been deleted/hidden */
    private boolean isDeleted;

    /** When the message was created */
    private Instant createdAt;

    /** When the message was last updated */
    private Instant updatedAt;

    /**
     * Convert LiveChatMessage entity to LiveChatMessageResponse DTO
     */
    public static LiveChatMessageResponse fromEntity(LiveChatMessage chatMessage) {
        return LiveChatMessageResponse.builder()
                .id(chatMessage.getId())
                .liveStreamId(chatMessage.getLiveStreamId())
                .senderId(chatMessage.getSenderId())
                .message(chatMessage.getMessage())
                .isSystemMessage(chatMessage.isSystemMessage())
                .isDeleted(chatMessage.isDeleted())
                .createdAt(chatMessage.getCreatedAt())
                .updatedAt(chatMessage.getUpdatedAt())
                .build();
    }
}
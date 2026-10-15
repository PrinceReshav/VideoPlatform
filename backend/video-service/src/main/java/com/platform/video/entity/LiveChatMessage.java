package com.platform.video.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for live chat messages (Feature 31: Live Streaming Platform)
 * Stores chat messages sent during live streams
 */
@Entity
@Table(name = "live_chat_messages", schema = "video_schema",
        indexes = {
                @Index(name = "idx_live_chat_live_stream", columnList = "live_stream_id"),
                @Index(name = "idx_live_chat_sender", columnList = "sender_id"),
                @Index(name = "idx_live_chat_created_at", columnList = "created_at")
        })
@Getter
@Setter
public class LiveChatMessage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The live stream this message belongs to */
    @Column(name = "live_stream_id", nullable = false)
    private UUID liveStreamId;

    /** The user who sent this message */
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    /** The message content */
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    /** Whether this is a system message (e.g., user joined/left) */
    @Column(name = "is_system_message", nullable = false)
    private boolean isSystemMessage = false;

    /** Whether this message has been deleted/hidden */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
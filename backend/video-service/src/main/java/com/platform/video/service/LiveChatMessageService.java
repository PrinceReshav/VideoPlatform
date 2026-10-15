package com.platform.video.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.video.entity.LiveChatMessage;
import com.platform.video.entity.LiveStream;
import com.platform.video.repository.LiveChatMessageRepository;
import com.platform.video.repository.LiveStreamRepository;
import com.platform.video.dto.LiveChatMessageRequest;
import com.platform.video.dto.LiveChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling live chat messages (Feature 31: Live Streaming Platform)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveChatMessageService {

    private final LiveChatMessageRepository liveChatMessageRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Send a chat message in a live stream
     */
    @Transactional
    public LiveChatMessageResponse sendChatMessage(UUID liveStreamId, LiveChatMessageRequest request) {
        UUID userId = currentUserAccessor.getCurrentUserId();

        // Verify the live stream exists and is online
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        if (liveStream.getStatus() != LiveStream.LiveStreamStatus.ONLINE) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Can only send messages to online live streams");
        }

        LiveChatMessage chatMessage = new LiveChatMessage();
        chatMessage.setLiveStreamId(liveStreamId);
        chatMessage.setSenderId(userId);
        chatMessage.setMessage(request.getMessage());
        chatMessage.setSystemMessage(false);
        chatMessage.setCreatedAt(Instant.now());
        chatMessage.setUpdatedAt(Instant.now());

        LiveChatMessage savedMessage = liveChatMessageRepository.save(chatMessage);
        log.info("Sent chat message {} in live stream {} by user {}", savedMessage.getId(), liveStreamId, userId);

        return LiveChatMessageResponse.fromEntity(savedMessage);
    }

    /**
     * Get chat messages for a live stream
     */
    @Transactional(readOnly = true)
    public List<LiveChatMessageResponse> getChatMessages(UUID liveStreamId, Integer limit) {
        // Verify the live stream exists
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        List<LiveChatMessage> messages;
        if (limit != null && limit > 0) {
            // Get recent messages with limit
            messages = liveChatMessageRepository.findByLiveStreamIdOrderByCreatedAtDesc(liveStreamId)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            // Get all messages
            messages = liveChatMessageRepository.findByLiveStreamIdOrderByCreatedAtDesc(liveStreamId);
        }

        return messages.stream()
                .map(LiveChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get new chat messages for a live stream since a given time
     */
    @Transactional(readOnly = true)
    public List<LiveChatMessageResponse> getNewChatMessages(UUID liveStreamId, Instant since) {
        // Verify the live stream exists
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        List<LiveChatMessage> messages = liveChatMessageRepository.findByLiveStreamIdAfter(liveStreamId, since);

        return messages.stream()
                .map(LiveChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Delete a chat message (soft delete)
     */
    @Transactional
    public void deleteChatMessage(UUID id) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        LiveChatMessage chatMessage = liveChatMessageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Chat message not found"));

        // Check if user owns this message or is the stream owner (for moderation)
        if (!chatMessage.getSenderId().equals(userId)) {
            // In a real implementation, you'd check if the user is a moderator
            // For now, only allow users to delete their own messages
            throw new BusinessException(ApiErrorCodes.FORBIDDEN, "You can only delete your own chat messages");
        }

        chatMessage.setDeleted(true);
        chatMessage.setUpdatedAt(Instant.now());
        liveChatMessageRepository.save(chatMessage);
        log.info("Deleted chat message {} by user {}", id, userId);
    }

    /**
     * Add a system message to a live stream (e.g., user joined/left)
     */
    @Transactional
    public LiveChatMessageResponse addSystemMessage(UUID liveStreamId, String message) {
        // Verify the live stream exists
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.NOT_FOUND, "Live stream not found"));

        LiveChatMessage chatMessage = new LiveChatMessage();
        chatMessage.setLiveStreamId(liveStreamId);
        // System messages don't have a sender (or could use a special system user ID)
        chatMessage.setSenderId(null);
        chatMessage.setMessage(message);
        chatMessage.setSystemMessage(true);
        chatMessage.setCreatedAt(Instant.now());
        chatMessage.setUpdatedAt(Instant.now());

        LiveChatMessage savedMessage = liveChatMessageRepository.save(chatMessage);
        log.info("Added system message {} to live stream {}", savedMessage.getId(), liveStreamId);

        return LiveChatMessageResponse.fromEntity(savedMessage);
    }
}
package com.platform.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating live chat messages (Feature 31: Live Streaming Platform)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatMessageRequest {

    /** The message content */
    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;
}
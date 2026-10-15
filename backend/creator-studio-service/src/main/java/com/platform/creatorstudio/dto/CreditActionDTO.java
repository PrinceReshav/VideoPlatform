package com.platform.creatorstudio.dto;

import java.util.UUID;

/**
 * Data Transfer Object for credit actions.
 * Used for receiving credit action data from clients (adding credits or posting comments).
 */
public class CreditActionDTO {

    private UUID userId;
    private UUID sessionId; // For comment posting
    private int amountPaise; // For adding credits
    private String commentText; // For posting comments
    private String description; // For adding credits

    // Constructors
    public CreditActionDTO() {}

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public int getAmountPaise() { return amountPaise; }
    public void setAmountPaise(int amountPaise) { this.amountPaise = amountPaise; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
package com.platform.creatorstudio.dto;

import com.platform.creatorstudio.model.LiveComment;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for comment information.
 * Used for transferring comment data between processes.
 */
public class CommentDTO {

    private UUID id;
    private UUID sessionId;
    private UUID userId;
    private String commentText;
    private int characterCount;
    private int blocksConsumed;
    private int pricePaise;
    private boolean isApproved;
    private boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;

    // Additional fields for response
    private long balancePaise;
    private double balanceRupees;

    // Constructors
    public CommentDTO() {}

    public CommentDTO(LiveComment comment) {
        this.id = comment.getId();
        this.sessionId = comment.getSessionId();
        this.userId = comment.getUserId();
        this.commentText = comment.getCommentText();
        this.characterCount = comment.getCharacterCount();
        this.blocksConsumed = comment.getBlocksConsumed();
        this.pricePaise = comment.getPricePaise();
        this.isApproved = comment.isApproved();
        this.isDeleted = comment.isDeleted();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public int getCharacterCount() { return characterCount; }
    public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }

    public int getBlocksConsumed() { return blocksConsumed; }
    public void setBlocksConsumed(int blocksConsumed) { this.blocksConsumed = blocksConsumed; }

    public int getPricePaise() { return pricePaise; }
    public void setPricePaise(int pricePaise) { this.pricePaise = pricePaise; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { this.isApproved = approved; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { this.isDeleted = deleted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public long getBalancePaise() { return balancePaise; }
    public void setBalancePaise(long balancePaise) { this.balancePaise = balancePaise; }

    public double getBalanceRupees() { return balanceRupees; }
    public void setBalanceRupees(double balanceRupees) { this.balanceRupees = balanceRupees; }
}
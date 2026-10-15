package com.platform.creatorstudio.model;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.text.BreakIterator;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Entity representing a comment made during a live studio session.
 * Implements the character-based pricing system: ₹5 for 200 characters.
 */
@Entity
@Table(name = "live_comments", schema = "creator_studio_schema",
        indexes = {
                @Index(name = "idx_live_comments_session", columnList = "session_id"),
                @Index(name = "idx_live_comments_user", columnList = "user_id"),
                @Index(name = "idx_live_comments_created", columnList = "created_at")
        })
@Getter
@Setter
public class LiveComment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The live session this comment belongs to */
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    /** The user who made the comment */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The comment text */
    @Column(name = "comment_text", length = 1000, nullable = false)
    private String commentText;

    /** Number of characters in the comment (for validation and billing) */
    @Column(name = "character_count", nullable = false)
    private int characterCount;

    /** Number of 200-character blocks this comment consumes */
    @Column(name = "blocks_consumed", nullable = false)
    private int blocksConsumed;

    /** Price paid for this comment (in paise) */
    @Column(name = "price_paise", nullable = false)
    private int pricePaise;

    /** Whether the comment has been moderated/approved */
    @Column(name = "is_approved", nullable = false)
    private boolean isApproved = true;

    /** Whether the comment has been deleted/hidden */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
// Comment moderation status
    public enum ModerationStatus {
        PENDING, APPROVED, REJECTED, DELETED
    }

    // Helper methods for character counting and pricing
    /**
     * Calculates the number of 200-character blocks needed for a given text.
     * Counts Unicode grapheme clusters (what users perceive as characters) rather than UTF-16 code units.
     * @param text The comment text
     * @return Number of 200-character blocks required
     */
    public static int calculateBlocksRequired(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Create a temporary LiveComment instance to use the grapheme counting method
        LiveComment temp = new LiveComment();
        int graphemeCount = temp.countGraphemeClusters(text);
        return (int) Math.ceil((double) graphemeCount / 200);
    }

    /**
     * Calculates the price in paise for a given number of blocks.
     * @param blocks Number of 200-character blocks
     * @return Price in paise (₹5 per block = 500 paise per block)
     */
    public static int calculatePricePaise(int blocks) {
        return blocks * 500; // ₹5 = 500 paise
    }

    /**
     * Validates if the comment text is within acceptable limits.
     * @param text The comment text to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidComment(String text) {
        if (text == null) {
            return false;
        }
        // Maximum comment length: 2000 characters (10 blocks)
        // Count Unicode grapheme clusters (what users perceive as characters) rather than UTF-16 code units
        LiveComment temp = new LiveComment();
        int graphemeCount = temp.countGraphemeClusters(text);
        return graphemeCount <= 2000 && !text.isBlank();
    }

    /**
     * Initializes a new LiveComment with calculated character count, blocks, and price.
     * This should be called before persisting the comment.
     * Counts Unicode grapheme clusters (what users perceive as characters) rather than UTF-16 code units.
     */
    public void initializeCommentMetrics() {
        if (this.commentText != null) {
            this.characterCount = countGraphemeClusters(this.commentText);
            this.blocksConsumed = calculateBlocksRequired(this.commentText);
            this.pricePaise = calculatePricePaise(this.blocksConsumed);
        }
    }

    /**
     * Counts Unicode grapheme clusters in the given text.
     * This represents what users perceive as individual characters.
     *
     * @param text The text to analyze
     * @return Number of grapheme clusters
     */
    private int countGraphemeClusters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);
        int count = 0;
        while (iterator.next() != BreakIterator.DONE) {
            count++;
        }
        return count;
    }

    // Helper methods
    public boolean isApprovedAndNotDeleted() {
        return isApproved && !isDeleted;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public int getBlocksConsumed() {
        return blocksConsumed;
    }

    public int getPricePaise() {
        return pricePaise;
    }
}
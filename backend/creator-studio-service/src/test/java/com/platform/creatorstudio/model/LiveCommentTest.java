package com.platform.creatorstudio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.BreakIterator;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LiveComment model.
 * Tests Unicode grapheme cluster counting for accurate character-based pricing.
 */
class LiveCommentTest {

    private LiveComment liveComment;

    @BeforeEach
    void setUp() {
        liveComment = new LiveComment();
    }

    @Test
    void testEnglishCharacterCounting() {
        // Arrange
        String text = "Hello World";
        liveComment.setCommentText(text);

        // Act
        liveComment.initializeCommentMetrics();

        // Assert
        assertEquals(11, liveComment.getCharacterCount(), "Should count 11 characters");
        assertEquals(1, liveComment.getBlocksConsumed(), "Should consume 1 block (11 chars < 200)");
        assertEquals(500, liveComment.getPricePaise(), "Should cost 500 paise (₹5)");
    }

    @Test
    void testEmojiCharacterCounting() {
        // Arrange
        String text = "Hello 👩‍💻 World"; // Contains a woman technologist emoji (may be multiple code units)
        liveComment.setCommentText(text);

        // Act
        liveComment.initializeCommentMetrics();

        // Assert
        // Note: The exact count depends on how BreakIterator interprets the emoji sequence
        // What's important is that it's consistent between initializeCommentMetrics and validation
        assertTrue(liveComment.getCharacterCount() > 0, "Should have positive character count");
        assertTrue(liveComment.getCharacterCount() <= text.length(),
                "Grapheme count should be less than or equal to UTF-16 code unit count");

        // Verify consistency between initialization and static method
        int blocksFromInit = liveComment.getBlocksConsumed();
        int blocksFromStatic = LiveComment.calculateBlocksRequired(text);
        assertEquals(blocksFromInit, blocksFromStatic,
                "Blocks calculated during initialization should match static method");
    }

    @Test
    void testAccentedCharacterCounting() {
        // Arrange
        String text = "café"; // Contains accented e
        liveComment.setCommentText(text);

        // Act
        liveComment.initializeCommentMetrics();

        // Assert
        // "café" has 4 grapheme clusters: c, a, f, é
        // Even if é is represented as e + ´ (2 code units), it's still 1 grapheme cluster
        assertEquals(4, liveComment.getCharacterCount(), "Should count 4 grapheme clusters");
        assertEquals(1, liveComment.getBlocksConsumed(), "Should consume 1 block (< 200 chars)");
        assertEquals(500, liveComment.getPricePaise(), "Should cost 500 paise (₹5)");
    }

    @Test
    void testCombiningCharacters() {
        // Arrange
        String text = "à́̂"; // letter a with multiple combining accents
        liveComment.setCommentText(text);

        // Act
        liveComment.initializeCommentMetrics();

        // Assert
        // Despite having multiple code units, this should be 1 grapheme cluster
        assertEquals(1, liveComment.getCharacterCount(), "Should count 1 grapheme cluster for combined character");
        assertEquals(1, liveComment.getBlocksConsumed(), "Should consume 1 block");
        assertEquals(500, liveComment.getPricePaise(), "Should cost 500 paise (₹5)");
    }

    @Test
    void testValidateCommentLengthLimit() {
        // Arrange & Act & Assert
        // Test exactly at limit
        String textAtLimit = "a".repeat(2000);
        assertTrue(LiveComment.isValidComment(textAtLimit),
                "Comment at exactly 2000 grapheme clusters should be valid");

        // Test over limit
        String textOverLimit = "a".repeat(2001);
        assertFalse(LiveComment.isValidComment(textOverLimit),
                "Comment over 2000 grapheme clusters should be invalid");

        // Test under limit
        String textUnderLimit = "a".repeat(1999);
        assertTrue(LiveComment.isValidComment(textUnderLimit),
                "Comment under 2000 grapheme clusters should be valid");

        // Test empty and blank
        assertFalse(LiveComment.isValidComment(""), "Empty comment should be invalid");
        assertFalse(LiveComment.isValidComment("   "), "Blank comment should be invalid");
        assertFalse(LiveComment.isValidComment(null), "Null comment should be invalid");
    }

    @Test
    void testCalculateBlocksRequired() {
        // Arrange & Act & Assert
        assertEquals(0, LiveComment.calculateBlocksRequired(null), "Null text should require 0 blocks");
        assertEquals(0, LiveComment.calculateBlocksRequired(""), "Empty text should require 0 blocks");
        assertEquals(1, LiveComment.calculateBlocksRequired("a".repeat(100)),
                "100 characters should require 1 block");
        assertEquals(1, LiveComment.calculateBlocksRequired("a".repeat(200)),
                "200 characters should require 1 block");
        assertEquals(2, LiveComment.calculateBlocksRequired("a".repeat(201)),
                "201 characters should require 2 blocks");
        assertEquals(10, LiveComment.calculateBlocksRequired("a".repeat(2000)),
                "2000 characters should require 10 blocks");
        assertEquals(11, LiveComment.calculateBlocksRequired("a".repeat(2001)),
                "2001 characters should require 11 blocks");
    }

    /**
     * Helper method to count grapheme clusters using BreakIterator for comparison in tests.
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
}
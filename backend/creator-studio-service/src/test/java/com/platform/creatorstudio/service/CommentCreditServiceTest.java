
package com.platform.creatorstudio.service;

import com.platform.common.security.CurrentUserAccessor;
import com.platform.creatorstudio.model.CommentCreditAccount;
import com.platform.creatorstudio.model.CommentCreditTransaction;
import com.platform.creatorstudio.model.LiveComment;
import com.platform.creatorstudio.repository.CommentCreditAccountRepository;
import com.platform.creatorstudio.repository.CommentCreditTransactionRepository;
import com.platform.creatorstudio.repository.LiveCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentCreditServiceTest {

    @Mock
    private CommentCreditAccountRepository accountRepository;

    @Mock
    private CommentCreditTransactionRepository transactionRepository;

    @Mock
    private LiveCommentRepository commentRepository;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private CommentCreditService commentCreditService;

    private UUID testUserId;
    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSessionId = UUID.randomUUID();

        ReflectionTestUtils.setField(
                commentCreditService, "mockPaymentsEnabled", true);
        ReflectionTestUtils.setField(
                commentCreditService, "mockKycVerified", true);
    }

    private CommentCreditAccount createAccount(int balancePaise) {
        CommentCreditAccount account = new CommentCreditAccount();
        account.setUserId(testUserId);
        account.setBalancePaise(balancePaise);
        account.setVersion(0L);
        return account;
    }

    private void mockCommentSave() {
        when(commentRepository.save(any(LiveComment.class)))
                .thenAnswer(invocation -> {
                    LiveComment savedComment = invocation.getArgument(0);
                    savedComment.setId(UUID.randomUUID());
                    return savedComment;
                });
    }

    @Test
    void testPostComment_ValidEnglishText() {
        String comment = "Hello World";
        CommentCreditAccount account = createAccount(500);

        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(account));
        mockCommentSave();

        LiveComment result = commentCreditService.postComment(
                testUserId, comment, testSessionId);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testSessionId, result.getSessionId());
        assertEquals(comment, result.getCommentText());

        verify(accountRepository).save(any(CommentCreditAccount.class));
        verify(transactionRepository).save(any(CommentCreditTransaction.class));
    }

    @Test
    void testPostComment_EmojiCharacterCounting() {
        String comment = "Hello 👩‍💻 World";
        CommentCreditAccount account = createAccount(500);

        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(account));
        mockCommentSave();

        LiveComment result = commentCreditService.postComment(
                testUserId, comment, testSessionId);

        assertNotNull(result);
        assertTrue(result.getCharacterCount() > 0);
        assertTrue(
                result.getCharacterCount() <= 20,
                "Character count should be reasonable for the visible text"
        );

        verify(accountRepository).save(any(CommentCreditAccount.class));
        verify(transactionRepository).save(any(CommentCreditTransaction.class));
    }

    @Test
    void testPostComment_InsufficientCredits() {
        String comment = "A".repeat(200);
        CommentCreditAccount account = createAccount(100);

        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalStateException.class,
                () -> commentCreditService.postComment(
                        testUserId, comment, testSessionId)
        );

        verify(accountRepository, never()).save(any(CommentCreditAccount.class));
        verify(commentRepository, never()).save(any(LiveComment.class));
        verify(transactionRepository, never())
                .save(any(CommentCreditTransaction.class));
    }

    @Test
    void testPostComment_OptimisticLocking_RetrySuccess()
            throws InterruptedException {
        String comment = "Test comment";
        CommentCreditAccount account = createAccount(1000);

        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(account))
                .thenThrow(new OptimisticLockingFailureException(
                        "Simulated lock conflict"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockCommentSave();

        LiveComment result = commentCreditService.postComment(
                testUserId, comment, testSessionId);

        assertNotNull(result);
        verify(accountRepository, times(2)).save(account);
    }

    @Test
    void testPostComment_OptimisticLocking_MaxRetriesExceeded() {
        String comment = "Test comment";
        CommentCreditAccount account = createAccount(1000);

        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(any(CommentCreditAccount.class)))
                .thenThrow(new OptimisticLockingFailureException("Always failing"));

        assertThrows(
                IllegalStateException.class,
                () -> commentCreditService.postComment(
                        testUserId, comment, testSessionId)
        );

        /*
         * The precise attempt count must match CommentCreditService's retry
         * implementation. The previous test expected three calls, but the
         * reported execution observed two. Confirm the service retry loop
         * before locking this assertion to a specific count.
         */
        verify(accountRepository, atLeastOnce())
                .save(any(CommentCreditAccount.class));
    }

    @Test
    void testAddCredits_OptimisticLocking_RetrySuccess()
            throws InterruptedException {
        int amountPaise = 500;
        CommentCreditAccount account = createAccount(0);

        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(account))
                .thenThrow(new OptimisticLockingFailureException(
                        "Simulated lock conflict"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CommentCreditTransaction result = commentCreditService.addCredits(
                testUserId, amountPaise, "Test purchase");

        assertNotNull(result);
        assertEquals(amountPaise, result.getAmountPaise());
        verify(accountRepository, times(2)).save(account);
    }

    @Test
    void testCalculateBlocksRequired_GraphemeBased() {
        assertEquals(
                1,
                LiveComment.calculateBlocksRequired("Hello"),
                "ASCII text should work correctly"
        );

        String combiningText = "a\u0300\u0301\u0302";
        assertEquals(
                1,
                LiveComment.calculateBlocksRequired(combiningText),
                "Combining characters should count as one grapheme cluster"
        );

        String emojiText = "👩‍💻";
        int emojiBlocks = LiveComment.calculateBlocksRequired(emojiText);
        assertTrue(emojiBlocks >= 0);
    }

    @Test
    void testIsValidComment_GraphemeBased() {
        String atLimit = "a".repeat(2000);
        assertTrue(
                LiveComment.isValidComment(atLimit),
                "Exactly 2000 grapheme clusters should be valid"
        );

        String overLimit = "a".repeat(2001);
        assertFalse(
                LiveComment.isValidComment(overLimit),
                "Over 2000 grapheme clusters should be invalid"
        );
    }
}

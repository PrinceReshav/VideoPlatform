package com.platform.creatorstudio.service;

import com.platform.creatorstudio.model.CommentCreditAccount;
import com.platform.creatorstudio.model.CommentCreditTransaction;
import com.platform.creatorstudio.model.LiveComment;
import com.platform.creatorstudio.repository.CommentCreditAccountRepository;
import com.platform.creatorstudio.repository.CommentCreditTransactionRepository;
import com.platform.creatorstudio.repository.LiveCommentRepository;
import com.platform.common.security.CurrentUserAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing comment credits.
 * Handles purchasing credits, consuming credits for comments,
 * and tracking comment credit transactions.
 *
 * In development, payment processing is static/mock.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentCreditService {

    private final CommentCreditAccountRepository accountRepository;
    private final CommentCreditTransactionRepository transactionRepository;
    private final LiveCommentRepository commentRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @Value("${creator-studio.payments.mock-enabled:false}")
    private boolean mockPaymentsEnabled;

    @Value("${creator-studio.kyc.mock-verified:false}")
    private boolean mockKycVerified;

    // Constants for the comment pricing system
    private static final int CHARACTERS_PER_BLOCK = 200;
    private static final int PRICE_PER_BLOCK_PAISE = 500; // ₹5 = 500 paise

    /**
     * Gets or creates a comment credit account for a user.
     *
     * @param userId The user ID
     * @return The credit account
     */
    public CommentCreditAccount getOrCreateCreditAccount(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CommentCreditAccount account = new CommentCreditAccount();
                    account.setUserId(userId);
                    account.setBalancePaise(0);
                    account.setTotalPurchasedPaise(0);
                    account.setTotalSpentPaise(0);
                    account.setCreatedAt(Instant.now());
                    account.setUpdatedAt(Instant.now());
                    return accountRepository.save(account);
                });
    }

    /**
     * Gets a user's comment credit account.
     *
     * @param userId The user ID
     * @return The credit account if found, null otherwise
     */
    public CommentCreditAccount getCreditAccount(UUID userId) {
        return accountRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Adds credits to a user's account (simulates payment purchase).
     * Uses optimistic locking to prevent race conditions in credit addition.
     *
     * @param userId The user ID
     * @param amountPaise The amount to add (in paise)
     * @param description Description of the transaction
     * @return The transaction record
     */
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public CommentCreditTransaction addCredits(UUID userId, int amountPaise, String description) {
        if (!mockPaymentsEnabled) {
            throw new IllegalStateException("Payment processing is not configured; credits cannot be added directly.");
        }
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                CommentCreditAccount account = getOrCreateCreditAccount(userId);
                long previousBalance = account.getBalancePaise();

                account.addCredits(amountPaise);
                accountRepository.save(account);

                // Create transaction record
                CommentCreditTransaction transaction = new CommentCreditTransaction();
                transaction.setUserId(userId);
                transaction.setTransactionType(CommentCreditTransaction.TransactionType.PURCHASE);
                transaction.setAmountPaise(amountPaise);
                transaction.setBalanceAfterPaise(account.getBalancePaise());
                transaction.setReferenceId(String.valueOf(userId));
                transaction.setReferenceType("USER");
                transaction.setDescription(description != null ? description : "Credit purchase");
                transaction.setCreatedAt(Instant.now());

                CommentCreditTransaction savedTransaction = transactionRepository.save(transaction);
                log.info("Credits added: {} paise for user {} (balance: {} -> {})",
                        amountPaise, userId, previousBalance, account.getBalancePaise());
                return savedTransaction;
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                // If this is the last attempt, rethrow the exception
                if (attempt == maxRetries - 1) {
                    log.warn("Failed to add credits after {} attempts due to concurrent modification", maxRetries);
                    throw new IllegalStateException("Unable to process credit transaction due to concurrent activity. Please try again.");
                }
                // Otherwise, retry the operation
                log.debug("Optimistic lock failure on attempt {}, retrying...", attempt + 1);
                // Small delay before retrying to reduce contention
                try {
                    Thread.sleep(10 * (attempt + 1)); // 10ms, 20ms, 30ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to retry credit transaction");
                }
            }
        }

        // This should never be reached due to the retry logic, but added for completeness
        throw new IllegalStateException("Unable to process credit transaction due to concurrent activity");
    }

    /**
     * Consumes credits for a comment (called when posting a comment).
     * Uses optimistic locking to prevent race conditions in credit deduction.
     *
     * @param userId The user ID posting the comment
     * @param comment The comment to post
     * @param sessionId The live session ID
     * @return The posted comment with transaction recorded
     */
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public LiveComment postComment(UUID userId, String comment, UUID sessionId) {
        // Validate the comment
        if (!LiveComment.isValidComment(comment)) {
            throw new IllegalArgumentException("Invalid comment: must be between 1 and 2000 characters");
        }

        // Check if user has KYC verification (static/mock for dev)
        if (!hasKycVerification(userId)) {
            throw new SecurityException("User must complete KYC verification to comment");
        }

        // Calculate required credits
        int blocksRequired = LiveComment.calculateBlocksRequired(comment);
        int requiredPaise = LiveComment.calculatePricePaise(blocksRequired);

        // Get or create the credit account with retry logic for optimistic locking
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                CommentCreditAccount account = getOrCreateCreditAccount(userId);

                // Check if user has sufficient credits
                if (!account.hasSufficientBalance(requiredPaise)) {
                    throw new IllegalStateException("Insufficient comment credits. Required: " + requiredPaise +
                            " paise, Available: " + account.getBalancePaise() + " paise");
                }

                // Deduct credits
                boolean deducted = account.deductCredits(requiredPaise);
                if (!deducted) {
                    throw new IllegalStateException("Failed to deduct credits");
                }

                // Save the account (will trigger optimistic lock check)
                accountRepository.save(account);

                // Create the comment
                LiveComment liveComment = new LiveComment();
                liveComment.setSessionId(sessionId);
                liveComment.setUserId(userId);
                liveComment.setCommentText(comment);
                liveComment.initializeCommentMetrics(); // Sets character count, blocks consumed, price
                liveComment.setCreatedAt(Instant.now());
                liveComment.setUpdatedAt(Instant.now());

                LiveComment savedComment = commentRepository.save(liveComment);

                // Create consumption transaction
                CommentCreditTransaction consumptionTransaction = new CommentCreditTransaction();
                consumptionTransaction.setUserId(userId);
                consumptionTransaction.setTransactionType(CommentCreditTransaction.TransactionType.CONSUMPTION);
                consumptionTransaction.setAmountPaise(requiredPaise);
                consumptionTransaction.setBalanceAfterPaise(account.getBalancePaise());
                consumptionTransaction.setReferenceId(savedComment.getId().toString());
                consumptionTransaction.setReferenceType("COMMENT");
                consumptionTransaction.setDescription("Comment posted: " +
                        (comment.length() > 50 ? comment.substring(0, 47) + "..." : comment));
                consumptionTransaction.setCreatedAt(Instant.now());

                transactionRepository.save(consumptionTransaction);

                log.info("Comment posted by user {} in session {}: {} paise consumed (balance: {})",
                        userId, sessionId, requiredPaise, account.getBalancePaise());
                return savedComment;
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                // If this is the last attempt, rethrow the exception
                if (attempt == maxRetries - 1) {
                    log.warn("Failed to post comment after {} attempts due to concurrent modification", maxRetries);
                    throw new IllegalStateException("Unable to process comment due to concurrent activity. Please try again.");
                }
                // Otherwise, retry the operation
                log.debug("Optimistic lock failure on attempt {}, retrying...", attempt + 1);
                // Small delay before retrying to reduce contention
                try {
                    Thread.sleep(10 * (attempt + 1)); // 10ms, 20ms, 30ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to retry comment posting");
                }
            }
        }

        // This should never be reached due to the retry logic, but added for completeness
        throw new IllegalStateException("Unable to process comment due to concurrent activity");
    }

    /**
     * Gets a user's comment credit balance.
     *
     * @param userId The user ID
     * @return The balance in paise
     */
    public long getCreditBalance(UUID userId) {
        CommentCreditAccount account = getCreditAccount(userId);
        return account != null ? account.getBalancePaise() : 0;
    }

    /**
     * Gets a user's comment credit transactions.
     *
     * @param userId The user ID
     * @return List of transactions
     */
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public List<CommentCreditTransaction> getCreditTransactions(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Gets recent comments for a live session.
     *
     * @param sessionId The session ID
     * @param limit Maximum number of comments to return
     * @return List of recent comments
     */
    public List<LiveComment> getRecentComments(UUID sessionId, int limit) {
        List<LiveComment> comments = commentRepository.findRecentApprovedCommentsBySession(sessionId);
        return comments.stream()
                .limit(limit > 0 ? limit : comments.size())
                .toList();
    }

    /**
     * Checks if a user has KYC verification.
     *
     * In development, this returns true for all users (static/mock).
     * In production, this would check against a verification service.
     *
     * @param userId The user ID
     * @return true if the user has KYC verification, false otherwise
     */
    public boolean hasKycVerification(UUID userId) {
        // No KYC provider is wired into this service yet. Fail closed unless an
        // explicitly configured development-only mock is enabled.
        return mockKycVerified;
    }

    /**
     * Gets the current user's ID from the authentication context.
     *
     * @return The current user's ID
     */
    protected UUID getCurrentUserId() {
        return currentUserAccessor.getCurrentUserId();
    }
}
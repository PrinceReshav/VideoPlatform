package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.CommentCreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CommentCreditTransaction entity.
 * Provides data access operations for comment credit transactions.
 */
@Repository
public interface CommentCreditTransactionRepository extends JpaRepository<CommentCreditTransaction, UUID> {

    /** Find transactions by user ID */
    List<CommentCreditTransaction> findByUserId(UUID userId);

    /** Find transactions by user ID and type */
    List<CommentCreditTransaction> findByUserIdAndTransactionType(UUID userId, com.platform.creatorstudio.model.CommentCreditTransaction.TransactionType type);

    /** Find recent transactions for a user */
    @Query("SELECT t FROM CommentCreditTransaction t WHERE " +
           "t.userId = :userId " +
           "ORDER BY t.createdAt DESC")
    List<CommentCreditTransaction> findRecentTransactionsByUser(
            @Param("userId") UUID userId);

    /** Get total purchased by user */
    @Query("SELECT SUM(t.amountPaise) FROM CommentCreditTransaction t WHERE " +
           "t.userId = :userId AND t.transactionType = :type")
    Long getTotalPurchasedByUser(@Param("userId") UUID userId,
                                @Param("type") com.platform.creatorstudio.model.CommentCreditTransaction.TransactionType type);

    /** Get total consumed by user */
    @Query("SELECT SUM(t.amountPaise) FROM CommentCreditTransaction t WHERE " +
           "t.userId = :userId AND t.transactionType = :type")
    Long getTotalConsumedByUser(@Param("userId") UUID userId,
                               @Param("type") com.platform.creatorstudio.model.CommentCreditTransaction.TransactionType type);

    /** Find transactions by reference ID and type */
    List<CommentCreditTransaction> findByReferenceIdAndReferenceType(String referenceId, String referenceType);
}
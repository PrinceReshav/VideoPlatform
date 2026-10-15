package com.platform.creatorstudio.model;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a transaction in a user's comment credit account.
 * Tracks purchases, refunds, and consumption of credits.
 */
@Entity
@Table(name = "comment_credit_transactions", schema = "creator_studio_schema",
        indexes = {
                @Index(name = "idx_comment_credit_transactions_user", columnList = "user_id"),
                @Index(name = "idx_comment_credit_transactions_type", columnList = "transaction_type"),
                @Index(name = "idx_comment_credit_transactions_created", columnList = "created_at")
        })
@Getter
@Setter
public class CommentCreditTransaction extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user this transaction belongs to */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Type of transaction (PURCHASE, REFUND, CONSUMPTION, etc.) */
    @Column(name = "transaction_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    /** Amount of credits involved in the transaction (in paise) */
    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    /** Balance after this transaction was applied */
    @Column(name = "balance_after_paise", nullable = false)
    private long balanceAfterPaise;

    /** Reference to related entity (e.g., comment ID for consumption) */
    @Column(name = "reference_id", length = 255)
    private String referenceId;

    /** Reference type (COMMENT, PURCHASE, etc.) */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /** Description of the transaction */
    @Column(name = "description", length = 500)
    private String description;
public enum TransactionType {
        PURCHASE,      // Credits purchased by user
        REFUND,        // Credits refunded to user
        CONSUMPTION,   // Credits consumed for commenting
        ADJUSTMENT,    // Manual adjustment (admin)
        BONUS          // Bonus credits awarded
    }

    // Helper methods
    public boolean isPurchase() {
        return transactionType == TransactionType.PURCHASE;
    }

    public boolean isConsumption() {
        return transactionType == TransactionType.CONSUMPTION;
    }

    public boolean isRefund() {
        return transactionType == TransactionType.REFUND;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public long getBalanceAfterPaise() {
        return balanceAfterPaise;
    }
}
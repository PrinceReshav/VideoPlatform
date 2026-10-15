package com.platform.creatorstudio.model;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user's comment credit account.
 * Tracks the balance of purchased credits for commenting on live sessions.
 */
@Entity
@Table(name = "comment_credit_accounts", schema = "creator_studio_schema",
        indexes = {
                @Index(name = "idx_comment_credit_accounts_user", columnList = "user_id", unique = true)
        })
@Getter
@Setter
public class CommentCreditAccount extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user this credit account belongs to */
    @Column(name = "user_id", nullable = false, updatable = false, unique = true)
    private UUID userId;

    /** Current credit balance (in paise for precise currency handling) */
    @Column(name = "balance_paise", nullable = false)
    private long balancePaise = 0; // Starts with zero balance

    /** Total credits purchased (lifetime) */
    @Column(name = "total_purchased_paise", nullable = false)
    private long totalPurchasedPaise = 0;

    /** Total credits spent (lifetime) */
    @Column(name = "total_spent_paise", nullable = false)
    private long totalSpentPaise = 0;
// Helper methods
    public boolean hasSufficientBalance(int requiredPaise) {
        return this.balancePaise >= requiredPaise;
    }

    public void addCredits(int amountPaise) {
        if (amountPaise > 0) {
            this.balancePaise += amountPaise;
            this.totalPurchasedPaise += amountPaise;
            setUpdatedAt(Instant.now());
        }
    }

    public boolean deductCredits(int amountPaise) {
        if (amountPaise > 0 && hasSufficientBalance(amountPaise)) {
            this.balancePaise -= amountPaise;
            this.totalSpentPaise += amountPaise;
            setUpdatedAt(Instant.now());
            return true;
        }
        return false;
    }

    public long getBalancePaise() {
        return balancePaise;
    }

    public BigDecimal getBalanceRupees() {
        return new BigDecimal(balancePaise).divide(new BigDecimal(100), 2);
    }

    public long getTotalPurchasedPaise() {
        return totalPurchasedPaise;
    }

    public long getTotalSpentPaise() {
        return totalSpentPaise;
    }

    public boolean isEmpty() {
        return balancePaise == 0;
    }
}
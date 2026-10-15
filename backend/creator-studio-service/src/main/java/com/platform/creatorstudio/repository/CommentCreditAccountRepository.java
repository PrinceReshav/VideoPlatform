package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.CommentCreditAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CommentCreditAccount entity.
 * Provides data access operations for comment credit accounts.
 */
@Repository
public interface CommentCreditAccountRepository extends JpaRepository<CommentCreditAccount, UUID> {

    /** Find credit account by user ID */
    Optional<CommentCreditAccount> findByUserId(UUID userId);

    /** Check if a credit account exists for a user */
    boolean existsByUserId(UUID userId);

    /** Find accounts with balance below a threshold */
    @Query("SELECT a FROM CommentCreditAccount a WHERE a.balancePaise < :threshold")
    java.util.List<CommentCreditAccount> findLowBalanceAccounts(@Param("threshold") long threshold);

    /** Find accounts with zero balance */
    java.util.List<CommentCreditAccount> findByBalancePaise(long balancePaise);

    /** Update balance for a user */
    @Modifying
    @Transactional
    @Query("UPDATE CommentCreditAccount a SET a.balancePaise = :balance, a.updatedAt = :now WHERE a.userId = :userId")
    void updateBalance(@Param("userId") UUID userId,
                      @Param("balance") long balance,
                      @Param("now") java.time.Instant now);
}
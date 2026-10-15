package com.platform.common.repository;

import com.platform.common.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Subscription entity (Feature 16: Subscribe/Follow System)
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /** Find subscriptions by subscriber ID */
    List<Subscription> findBySubscriberId(UUID subscriberId);

    /** Find subscriptions by target ID and target type */
    List<Subscription> findByTargetIdAndTargetType(UUID targetId, String targetType);

    /** Find subscriptions by target ID, target type, and subscription type */
    List<Subscription> findByTargetIdAndTargetTypeAndSubscriptionType(UUID targetId, String targetType, String subscriptionType);

    /** Find active subscriptions by target ID and target type */
    List<Subscription> findByTargetIdAndTargetTypeAndActiveTrue(UUID targetId, String targetType);

    /** Find subscriptions by subscriber ID and target type */
    List<Subscription> findBySubscriberIdAndTargetType(UUID subscriberId, String targetType);

    /** Find subscriptions by timestamp range */
    List<Subscription> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /** Find subscriptions by multiple criteria */
    @Query("SELECT s FROM Subscription s WHERE " +
            "(:subscriberId IS NULL OR s.subscriberId = :subscriberId) AND " +
            "(:targetId IS NULL OR s.targetId = :targetId) AND " +
            "(:targetType IS NULL OR s.targetType = :targetType) AND " +
            "(:subscriptionType IS NULL OR s.subscriptionType = :subscriptionType) AND " +
            "(:active IS NULL OR s.active = :active) AND " +
            "(:startTime IS NULL OR s.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR s.createdAt <= :endTime)")
    Page<Subscription> findByCriteria(
            @Param("subscriberId") UUID subscriberId,
            @Param("targetId") UUID targetId,
            @Param("targetType") String targetType,
            @Param("subscriptionType") String subscriptionType,
            @Param("active") Boolean active,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
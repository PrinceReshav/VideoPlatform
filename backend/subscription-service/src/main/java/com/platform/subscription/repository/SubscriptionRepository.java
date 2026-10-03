package com.platform.subscription.repository;

import com.platform.subscription.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserIdAndChannelId(UUID userId, UUID channelId);

    long countByChannelId(UUID channelId);

    long countByUserId(UUID userId);

    Page<Subscription> findByUserId(UUID userId, Pageable pageable);

    Page<Subscription> findByChannelId(UUID channelId, Pageable pageable);

    List<Subscription> findByChannelId(UUID channelId);

    List<Subscription> findByUserIdAndNotificationsEnabledTrue(UUID userId);

    @Query("SELECT s.channelId FROM Subscription s WHERE s.userId = :userId")
    List<UUID> findSubscribedChannelIds(@Param("userId") UUID userId);

    boolean existsByUserIdAndChannelId(UUID userId, UUID channelId);
}
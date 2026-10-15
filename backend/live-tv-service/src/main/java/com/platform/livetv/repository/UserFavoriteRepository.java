package com.platform.livetv.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class UserFavoriteRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public List<UUID> findFavoriteChannelIds(UUID userId, Collection<UUID> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) return List.of();
        return entityManager.createNativeQuery(
                "SELECT channel_id FROM live_tv_schema.user_favorites WHERE user_id = :userId AND channel_id IN (:channelIds)")
                .setParameter("userId", userId)
                .setParameter("channelIds", channelIds)
                .getResultList();
    }

    public boolean exists(UUID userId, UUID channelId) {
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM live_tv_schema.user_favorites WHERE user_id = :userId AND channel_id = :channelId")
                .setParameter("userId", userId)
                .setParameter("channelId", channelId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    public void add(UUID userId, UUID channelId) {
        entityManager.createNativeQuery(
                "INSERT INTO live_tv_schema.user_favorites (user_id, channel_id) VALUES (:userId, :channelId) ON CONFLICT (user_id, channel_id) DO NOTHING")
                .setParameter("userId", userId)
                .setParameter("channelId", channelId)
                .executeUpdate();
    }

    public void remove(UUID userId, UUID channelId) {
        entityManager.createNativeQuery(
                "DELETE FROM live_tv_schema.user_favorites WHERE user_id = :userId AND channel_id = :channelId")
                .setParameter("userId", userId)
                .setParameter("channelId", channelId)
                .executeUpdate();
    }
}

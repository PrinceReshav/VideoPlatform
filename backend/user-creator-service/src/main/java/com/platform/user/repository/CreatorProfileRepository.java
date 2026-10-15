package com.platform.user.repository;

import com.platform.user.entity.Channel;
import com.platform.user.entity.CreatorProfile;
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
public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, UUID> {

    Optional<CreatorProfile> findByUserId(UUID userId);

    Optional<CreatorProfile> findByChannelSlug(String channelSlug);

    Optional<CreatorProfile> findByChannelName(String channelName);

    boolean existsByUserId(UUID userId);

    boolean existsByChannelName(String channelName);

    boolean existsByChannelSlug(String channelSlug);

    @Query("SELECT cp FROM CreatorProfile cp WHERE cp.active = true AND cp.deleted = false AND cp.suspended = false")
    List<CreatorProfile> findActiveCreators();

    Page<CreatorProfile> findByActiveTrueAndDeletedFalseAndSuspendedFalse(Pageable pageable);


    @Query("""
    SELECT cp
    FROM CreatorProfile cp
    WHERE cp.category.id = :categoryId
      AND cp.active = true
      AND cp.deleted = false
      AND cp.suspended = false
    """)
    List<CreatorProfile> findByCategory(
            @Param("categoryId") UUID categoryId
    );

}
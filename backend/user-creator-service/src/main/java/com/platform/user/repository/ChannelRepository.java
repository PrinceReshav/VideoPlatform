
package com.platform.user.repository;

import com.platform.user.entity.Channel;
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
public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    Optional<Channel> findBySlug(String slug);

    Optional<Channel> findByCustomUrl(String customUrl);

    Optional<Channel> findByCreatorProfileId(UUID creatorProfileId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByCustomUrl(String customUrl);

    boolean existsByCreatorProfileId(UUID creatorProfileId);

    boolean existsByName(String name);

    @Query("""
        SELECT c FROM Channel c
        WHERE c.active = true
          AND c.deleted = false
          AND c.suspended = false
        """)
    List<Channel> findActiveChannels();

    Page<Channel> findByActiveTrueAndDeletedFalseAndSuspendedFalse(
            Pageable pageable
    );

    @Query("""
        SELECT c FROM Channel c
        WHERE c.category.id = :categoryId
          AND c.active = true
          AND c.deleted = false
          AND c.suspended = false
        """)
    List<Channel> findByCategory(@Param("categoryId") UUID categoryId);

    @Query("""
        SELECT c FROM Channel c
        WHERE c.verified = true
          AND c.active = true
          AND c.deleted = false
          AND c.suspended = false
        """)
    List<Channel> findVerifiedChannels();
}

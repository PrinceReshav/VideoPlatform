package com.platform.creatorstudio.repository;

import com.platform.creatorstudio.model.StudioLiveSessionAttendee;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudioLiveSessionAttendeeRepository extends JpaRepository<StudioLiveSessionAttendee, UUID> {
    boolean existsBySessionIdAndUserId(UUID sessionId, UUID userId);
    Optional<StudioLiveSessionAttendee> findBySessionIdAndUserId(UUID sessionId, UUID userId);
    long countBySessionId(UUID sessionId);
}

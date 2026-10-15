package com.platform.livetv.repository;

import com.platform.livetv.entity.LiveStreamCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LiveStreamCandidateRepository extends JpaRepository<LiveStreamCandidate, UUID> {
    List<LiveStreamCandidate> findAllByChannelIdOrderByIdAsc(UUID channelId);
    void deleteAllByChannelId(UUID channelId);
    long countByChannelId(UUID channelId);
}

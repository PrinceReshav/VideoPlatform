package com.platform.upload.repository;

import com.platform.upload.entity.UploadChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadChunkRepository extends JpaRepository<UploadChunk, UUID> {

    List<UploadChunk> findBySessionId(UUID sessionId);

    Optional<UploadChunk> findBySessionIdAndPartNumber(UUID sessionId, Integer partNumber);

    List<UploadChunk> findBySessionIdOrderByPartNumberAsc(UUID sessionId);

    void deleteBySessionId(UUID sessionId);

    long countBySessionId(UUID sessionId);
}
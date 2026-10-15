package com.platform.livetv.repository;

import com.platform.livetv.entity.EpgProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EpgProgramRepository extends JpaRepository<EpgProgram, UUID> {
    @Query("SELECT p FROM EpgProgram p WHERE p.channelId = :channelId AND p.deleted = false AND p.startTime < :endTime AND p.endTime > :startTime ORDER BY p.startTime ASC")
    List<EpgProgram> findForWindow(@Param("channelId") UUID channelId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}

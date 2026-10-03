package com.platform.common.repository;

import com.platform.common.entity.ProcessingJob;
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
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob> findByVideoId(UUID videoId);

    Optional<ProcessingJob> findByVideoIdAndStatusIn(UUID videoId, List<ProcessingJob.Status> statuses);

    List<ProcessingJob> findByStatus(ProcessingJob.Status status);

    @Query("SELECT pj FROM ProcessingJob pj WHERE pj.status IN (:statuses) ORDER BY pj.priority DESC, pj.createdAt ASC")
    List<ProcessingJob> findByStatusIn(@Param("statuses") List<ProcessingJob.Status> statuses);

    List<ProcessingJob> findByVideoIdAndStatusOrderByCreatedAtDesc(UUID videoId, ProcessingJob.Status status);

    @Query("SELECT pj FROM ProcessingJob pj WHERE pj.workerId = :workerId AND pj.status = 'PROCESSING'")
    List<ProcessingJob> findByWorkerIdAndProcessing(@Param("workerId") String workerId);

    Page<ProcessingJob> findByStatus(ProcessingJob.Status status, Pageable pageable);
}
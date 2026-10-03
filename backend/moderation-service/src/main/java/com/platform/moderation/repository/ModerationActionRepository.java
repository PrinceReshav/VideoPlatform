package com.platform.moderation.repository;

import com.platform.moderation.entity.ModerationAction;
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
public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    List<ModerationAction> findByReportId(UUID reportId);

    List<ModerationAction> findByModeratorId(UUID moderatorId);

    List<ModerationAction> findByTargetTypeAndTargetId(ModerationAction.TargetType targetType, UUID targetId);

    Page<ModerationAction> findByModeratorId(UUID moderatorId, Pageable pageable);
}
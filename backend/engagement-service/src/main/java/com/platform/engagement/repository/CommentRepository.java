package com.platform.engagement.repository;

import com.platform.engagement.entity.Comment;
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
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByVideoIdAndHiddenFalse(UUID videoId, Pageable pageable);

    List<Comment> findByVideoIdAndParentIdIsNullAndHiddenFalseOrderByCreatedAtDesc(UUID videoId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    long countByVideoIdAndHiddenFalse(UUID videoId);

    Optional<Comment> findByIdAndVideoId(UUID id, UUID videoId);

    List<Comment> findByUserId(UUID userId);
}
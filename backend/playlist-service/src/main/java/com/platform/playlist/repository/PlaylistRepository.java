package com.platform.playlist.repository;

import com.platform.playlist.entity.Playlist;
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
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    List<Playlist> findByUserId(UUID userId);

    Page<Playlist> findByUserId(UUID userId, Pageable pageable);

    List<Playlist> findByUserIdAndVisibility(UUID userId, Playlist.Visibility visibility);

    Page<Playlist> findByVisibility(Playlist.Visibility visibility, Pageable pageable);

    Optional<Playlist> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
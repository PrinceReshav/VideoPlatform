package com.platform.user.repository;

import com.platform.user.entity.Genre;
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
public interface GenreRepository extends JpaRepository<Genre, UUID> {

    Optional<Genre> findBySlug(String slug);

    Optional<Genre> findByName(String name);

    List<Genre> findByParent_Id(UUID parentId);

    List<Genre> findByParentIdIsNull();

    @Query("SELECT g FROM Genre g WHERE g.active = true AND g.deleted = false ORDER BY g.sortOrder, g.name")
    List<Genre> findActiveRootGenres();

    @Query("""
    SELECT g
    FROM Genre g
    WHERE g.active = true
      AND g.deleted = false
      AND g.parent.id = :parentId
    ORDER BY g.sortOrder, g.name
    """)
    List<Genre> findActiveChildren(@Param("parentId") UUID parentId);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    Page<Genre> findByActiveTrueAndDeletedFalse(Pageable pageable);
}
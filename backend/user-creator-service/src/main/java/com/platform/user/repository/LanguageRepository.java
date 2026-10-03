package com.platform.user.repository;

import com.platform.user.entity.Language;
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
public interface LanguageRepository extends JpaRepository<Language, UUID> {

    Optional<Language> findByCode(String code);

    Optional<Language> findByName(String name);

    @Query("SELECT l FROM Language l WHERE l.active = true AND l.deleted = false ORDER BY l.name")
    List<Language> findActiveLanguages();

    boolean existsByCode(String code);

    boolean existsByName(String name);

    Page<Language> findByActiveTrueAndDeletedFalse(Pageable pageable);
}
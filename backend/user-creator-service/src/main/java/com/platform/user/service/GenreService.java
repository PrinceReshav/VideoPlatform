package com.platform.user.service;

import com.platform.user.dto.*;
import com.platform.user.entity.*;
import com.platform.user.repository.*;
import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ConflictException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.util.SlugUtil;
import com.platform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;
    private final UserMapper userMapper;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional(readOnly = true)
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findByParentIdIsNull().stream()
                .filter(Genre::isActive)
                .filter(g -> !g.isDeleted())
                .map(userMapper::toGenreResponseWithChildren)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GenreResponse> getActiveRootGenres() {
        return genreRepository.findActiveRootGenres().stream()
                .map(userMapper::toGenreResponseWithChildren)
                .toList();
    }

    @Transactional(readOnly = true)
    public GenreResponse getGenreById(UUID id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Genre not found"));
        return userMapper.toGenreResponseWithChildren(genre);
    }

    @Transactional(readOnly = true)
    public GenreResponse getGenreBySlug(String slug) {
        Genre genre = genreRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Genre not found"));
        return userMapper.toGenreResponseWithChildren(genre);
    }

    @Transactional
    public GenreResponse createGenre(String name, String description, UUID parentId, int sortOrder, boolean system) {
        String slug = SlugUtil.slugify(name);
        String uniqueSlug = SlugUtil.generateUniqueSlug(slug, s -> genreRepository.existsBySlug(s));

        Genre genre = new Genre();
        genre.setName(name);
        genre.setSlug(uniqueSlug);
        genre.setDescription(description);
        genre.setSortOrder(sortOrder);
        genre.setSystem(system);
        genre.setActive(true);

        if (parentId != null) {
            Genre parent = genreRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Parent genre not found"));
            genre.setParent(parent);
        }

        genre = genreRepository.save(genre);
        log.info("Created genre: {} ({})", genre.getName(), genre.getId());
        return userMapper.toGenreResponse(genre);
    }

    @Transactional
    public GenreResponse updateGenre(UUID id, String name, String description, UUID parentId,
                                      Integer sortOrder, Boolean active) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Genre not found"));

        if (genre.isSystem()) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_LOCKED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Cannot modify system genre");
        }

        if (name != null && !name.equals(genre.getName())) {
            String slug = SlugUtil.slugify(name);
            String uniqueSlug = SlugUtil.generateUniqueSlug(slug, s -> genreRepository.existsBySlug(s));
            genre.setName(name);
            genre.setSlug(uniqueSlug);
        }

        if (description != null) genre.setDescription(description);
        if (sortOrder != null) genre.setSortOrder(sortOrder);
        if (active != null) genre.setActive(active);

        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Genre cannot be its own parent");
            }
            Genre parent = genreRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Parent genre not found"));
            genre.setParent(parent);
        } else if (parentId != null && parentId.equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            genre.setParent(null);
        }

        genre = genreRepository.save(genre);
        log.info("Updated genre: {} ({})", genre.getName(), genre.getId());
        return userMapper.toGenreResponse(genre);
    }

    @Transactional
    public void deleteGenre(UUID id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Genre not found"));

        if (genre.isSystem()) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_LOCKED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Cannot delete system genre");
        }

        if (!genre.getChildren().isEmpty()) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_CONFLICT,
                    org.springframework.http.HttpStatus.CONFLICT, "Genre has children, delete them first");
        }

        genre.markDeleted(currentUserAccessor.getCurrentUserId());
        genreRepository.save(genre);
        log.info("Deleted genre: {} ({})", genre.getName(), genre.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<GenreResponse> getGenres(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Genre> genrePage = genreRepository.findByActiveTrueAndDeletedFalse(pageable);
        return PageResponse.of(
                genrePage.getContent().stream().map(userMapper::toGenreResponse).toList(),
                page, size, genrePage.getTotalElements()
        );
    }
}
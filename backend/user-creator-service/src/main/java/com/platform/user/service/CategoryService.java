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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserMapper userMapper;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByParentIdIsNull().stream()
                .filter(Category::isActive)
                .filter(c -> !c.isDeleted())
                .map(userMapper::toCategoryResponseWithChildren)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveRootCategories() {
        return categoryRepository.findActiveRootCategories().stream()
                .map(userMapper::toCategoryResponseWithChildren)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));
        return userMapper.toCategoryResponseWithChildren(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));
        return userMapper.toCategoryResponseWithChildren(category);
    }

    @Transactional
    public CategoryResponse createCategory(String name, String description, String icon, String color,
                                            UUID parentId, int sortOrder, boolean system) {
        String slug = SlugUtil.slugify(name);
        String uniqueSlug = SlugUtil.generateUniqueSlug(slug, s -> categoryRepository.existsBySlug(s));

        Category category = new Category();
        category.setName(name);
        category.setSlug(uniqueSlug);
        category.setDescription(description);
        category.setIcon(icon);
        category.setColor(color);
        category.setSortOrder(sortOrder);
        category.setSystem(system);
        category.setActive(true);

        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Parent category not found"));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        log.info("Created category: {} ({})", category.getName(), category.getId());
        return userMapper.toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, String name, String description, String icon,
                                            String color, UUID parentId, Integer sortOrder, Boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));

        if (category.isSystem()) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_LOCKED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Cannot modify system category");
        }

        if (name != null && !name.equals(category.getName())) {
            String slug = SlugUtil.slugify(name);
            String uniqueSlug = SlugUtil.generateUniqueSlug(slug, s -> categoryRepository.existsBySlug(s));
            category.setName(name);
            category.setSlug(uniqueSlug);
        }

        if (description != null) category.setDescription(description);
        if (icon != null) category.setIcon(icon);
        if (color != null) category.setColor(color);
        if (sortOrder != null) category.setSortOrder(sortOrder);
        if (active != null) category.setActive(active);

        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Parent category not found"));
            category.setParent(parent);
        } else if (parentId != null && parentId.equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            category.setParent(null);
        }

        category = categoryRepository.save(category);
        log.info("Updated category: {} ({})", category.getName(), category.getId());
        return userMapper.toCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Category not found"));

        if (category.isSystem()) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_LOCKED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Cannot delete system category");
        }

        if (!category.getChildren().isEmpty()) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_CONFLICT,
                    org.springframework.http.HttpStatus.CONFLICT, "Category has children, delete them first");
        }

        category.markDeleted(currentUserAccessor.getCurrentUserId());
        categoryRepository.save(category);
        log.info("Deleted category: {} ({})", category.getName(), category.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = categoryRepository.findByActiveTrueAndDeletedFalse(pageable);
        return PageResponse.of(
                categoryPage.getContent().stream().map(userMapper::toCategoryResponse).toList(),
                page, size, categoryPage.getTotalElements()
        );
    }
}
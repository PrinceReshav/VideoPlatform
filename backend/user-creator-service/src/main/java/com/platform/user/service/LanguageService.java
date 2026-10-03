package com.platform.user.service;

import com.platform.user.dto.LanguageResponse;
import com.platform.user.entity.Language;
import com.platform.user.repository.LanguageRepository;
import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.dto.PageResponse;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ConflictException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final UserMapper userMapper;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional(readOnly = true)
    public List<LanguageResponse> getActiveLanguages() {
        return languageRepository.findActiveLanguages().stream()
                .map(userMapper::toLanguageResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LanguageResponse getLanguageById(UUID id) {
        Language language = languageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));
        return userMapper.toLanguageResponse(language);
    }

    @Transactional(readOnly = true)
    public LanguageResponse getLanguageByCode(String code) {
        Language language = languageRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));
        return userMapper.toLanguageResponse(language);
    }

    @Transactional
    public LanguageResponse createLanguage(String code, String name, String nativeName, boolean system) {
        if (languageRepository.existsByCode(code)) {
            throw new ConflictException(ApiErrorCodes.RESOURCE_ALREADY_EXISTS, "Language code already exists");
        }
        if (languageRepository.existsByName(name)) {
            throw new ConflictException(ApiErrorCodes.RESOURCE_ALREADY_EXISTS, "Language name already exists");
        }

        Language language = new Language();
        language.setCode(code.toLowerCase());
        language.setName(name);
        language.setNativeName(nativeName);
        language.setSystem(system);
        language.setActive(true);

        language = languageRepository.save(language);
        log.info("Created language: {} ({})", language.getName(), language.getId());
        return userMapper.toLanguageResponse(language);
    }

    @Transactional
    public LanguageResponse updateLanguage(UUID id, String name, String nativeName, Boolean active) {
        Language language = languageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));

        if (language.isSystem()) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_LOCKED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Cannot modify system language");
        }

        if (name != null) language.setName(name);
        if (nativeName != null) language.setNativeName(nativeName);
        if (active != null) language.setActive(active);

        language = languageRepository.save(language);
        log.info("Updated language: {} ({})", language.getName(), language.getId());
        return userMapper.toLanguageResponse(language);
    }

    @Transactional
    public void deleteLanguage(UUID id) {
        Language language = languageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Language not found"));

        if (language.isSystem()) {
            throw new BusinessException(ApiErrorCodes.RESOURCE_LOCKED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Cannot delete system language");
        }

        language.markDeleted(currentUserAccessor.getCurrentUserId());
        languageRepository.save(language);
        log.info("Deleted language: {} ({})", language.getName(), language.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<LanguageResponse> getLanguages(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Language> languagePage = languageRepository.findByActiveTrueAndDeletedFalse(pageable);
        return PageResponse.of(
                languagePage.getContent().stream().map(userMapper::toLanguageResponse).toList(),
                page, size, languagePage.getTotalElements()
        );
    }
}
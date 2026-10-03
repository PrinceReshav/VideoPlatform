package com.platform.user.controller;

import com.platform.user.dto.*;
import com.platform.user.service.LanguageService;
import com.platform.common.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    public ResponseEntity<List<LanguageResponse>> getAllLanguages() {
        return ResponseEntity.ok(languageService.getActiveLanguages());
    }

    @GetMapping("/page")
    public ResponseEntity<PageResponse<LanguageResponse>> getLanguagesPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(languageService.getLanguages(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LanguageResponse> getLanguage(@PathVariable UUID id) {
        return ResponseEntity.ok(languageService.getLanguageById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<LanguageResponse> getLanguageByCode(@PathVariable String code) {
        return ResponseEntity.ok(languageService.getLanguageByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LanguageResponse> createLanguage(
            @Valid @RequestBody CreateLanguageRequest request) {
        LanguageResponse response = languageService.createLanguage(
                request.getCode(), request.getName(), request.getNativeName(), request.isSystem()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LanguageResponse> updateLanguage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLanguageRequest request) {
        LanguageResponse response = languageService.updateLanguage(
                id, request.getName(), request.getNativeName(), request.getActive()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLanguage(@PathVariable UUID id) {
        languageService.deleteLanguage(id);
        return ResponseEntity.ok().build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLanguageRequest {
        @NotBlank(message = "Code is required")
        @Size(max = 10)
        private String code;

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        private String name;

        @Size(max = 100)
        private String nativeName;

        private boolean system = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateLanguageRequest {
        @Size(min = 2, max = 100)
        private String name;

        @Size(max = 100)
        private String nativeName;

        private Boolean active;
    }
}
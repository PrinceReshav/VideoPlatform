package com.platform.user.controller;

import com.platform.user.dto.*;
import com.platform.user.service.GenreService;
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
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }

    @GetMapping("/active")
    public ResponseEntity<List<GenreResponse>> getActiveGenres() {
        return ResponseEntity.ok(genreService.getActiveRootGenres());
    }

    @GetMapping("/page")
    public ResponseEntity<PageResponse<GenreResponse>> getGenresPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(genreService.getGenres(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenreResponse> getGenre(@PathVariable UUID id) {
        return ResponseEntity.ok(genreService.getGenreById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<GenreResponse> getGenreBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(genreService.getGenreBySlug(slug));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenreResponse> createGenre(
            @Valid @RequestBody CreateGenreRequest request) {
        GenreResponse response = genreService.createGenre(
                request.getName(), request.getDescription(), request.getParentId(),
                request.getSortOrder(), request.isSystem()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenreResponse> updateGenre(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGenreRequest request) {
        GenreResponse response = genreService.updateGenre(
                id, request.getName(), request.getDescription(), request.getParentId(),
                request.getSortOrder(), request.getActive()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGenre(@PathVariable UUID id) {
        genreService.deleteGenre(id);
        return ResponseEntity.ok().build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGenreRequest {
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        private UUID parentId;

        @Min(0)
        private int sortOrder = 0;

        private boolean system = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateGenreRequest {
        @Size(min = 2, max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        private UUID parentId;

        @Min(0)
        private Integer sortOrder;

        private Boolean active;
    }
}
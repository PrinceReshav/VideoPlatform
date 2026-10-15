package com.platform.creatorstudio.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.creatorstudio.dto.StudioCreateDTO;
import com.platform.creatorstudio.dto.StudioDTO;
import com.platform.creatorstudio.model.CreatorStudio;
import com.platform.creatorstudio.model.CreatorStudio.OwnerType;
import com.platform.creatorstudio.model.StudioCategory;
import com.platform.creatorstudio.service.CreatorStudioService;
import com.platform.creatorstudio.service.StudioNameAvailabilityService;
import com.platform.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creator-studios")
@RequiredArgsConstructor
@Validated
@Tag(name = "Creator Studios", description = "Create and manage creator workspaces")
@SecurityRequirement(name = "Bearer Authentication")
public class CreatorStudioController {

    private final CreatorStudioService studioService;
    private final StudioNameAvailabilityService nameAvailabilityService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my studios")
    public ResponseEntity<com.platform.common.dto.ApiResponse<List<StudioDTO>>> getMyStudios() {
        return ResponseEntity.ok(ApiResponse.success(
                studioService.getStudiosByCurrentUser().stream().map(StudioDTO::new).toList()));
    }

    @GetMapping("/name-availability")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check whether a studio name is available")
    public ResponseEntity<com.platform.common.dto.ApiResponse<Map<String, Object>>> checkName(
            @RequestParam @NotBlank String name) {
        String normalized = name.trim();
        boolean valid = normalized.length() >= 3 && normalized.length() <= 100
                && normalized.matches("[A-Za-z0-9][A-Za-z0-9._ -]*");
        boolean available = valid && nameAvailabilityService.isStudioNameAvailable(normalized);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "name", normalized,
                "valid", valid,
                "available", available)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a creator studio")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Studio created",
                    content = @Content(schema = @Schema(implementation = StudioDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid studio details"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Studio name is already in use")
    })
    public ResponseEntity<com.platform.common.dto.ApiResponse<StudioDTO>> createStudio(
            @Valid @RequestBody StudioCreateDTO request) {
        final StudioCategory category;
        final OwnerType ownerType;
        try {
            category = StudioCategory.valueOf(request.getPrimaryCategory().trim().toUpperCase());
            ownerType = request.getOwnerType() == null || request.getOwnerType().isBlank()
                    ? OwnerType.INDIVIDUAL
                    : OwnerType.valueOf(request.getOwnerType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Unknown primaryCategory or ownerType. Use a supported studio category and INDIVIDUAL or TEAM."));
        }

        try {
            CreatorStudio created = studioService.createStudio(
                    request.getName(), request.getDisplayName(), request.getDescription(),
                    request.getAvatarUrl(), request.getBannerUrl(), category, ownerType);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(new StudioDTO(created)));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("That studio name is already in use."));
        }
    }

    @GetMapping("/{studioId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a studio owned by the current user")
    public ResponseEntity<com.platform.common.dto.ApiResponse<StudioDTO>> getStudio(
            @PathVariable UUID studioId) {
        CreatorStudio studio = studioService.getStudioById(studioId);
        if (studio == null || (!studio.isActive() || studio.isDeleted())) {
            return ResponseEntity.notFound().build();
        }
        UUID currentUserId = currentUserAccessor.getCurrentUserId();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !studio.getOwnerId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have access to this studio."));
        }
        return ResponseEntity.ok(ApiResponse.success(new StudioDTO(studio)));
    }

    @PatchMapping("/{studioId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update studio details")
    public ResponseEntity<com.platform.common.dto.ApiResponse<StudioDTO>> updateStudio(
            @PathVariable UUID studioId, @RequestBody StudioCreateDTO request) {
        StudioCategory category = null;
        if (request.getPrimaryCategory() != null && !request.getPrimaryCategory().isBlank()) {
            try {
                category = StudioCategory.valueOf(request.getPrimaryCategory().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Unsupported primaryCategory."));
            }
        }
        CreatorStudio updated = studioService.updateStudio(studioId, request.getDisplayName(),
                request.getDescription(), request.getAvatarUrl(), request.getBannerUrl(), category);
        return ResponseEntity.ok(ApiResponse.success(new StudioDTO(updated)));
    }

    @DeleteMapping("/{studioId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deactivate a studio")
    public ResponseEntity<com.platform.common.dto.ApiResponse<Void>> deactivateStudio(
            @PathVariable UUID studioId) {
        studioService.deactivateStudio(studioId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

package com.platform.creatorstudio.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.creatorstudio.dto.ContentDTO;
import com.platform.creatorstudio.dto.ContentActionDTO;
import com.platform.creatorstudio.service.StudioContentService;
import com.platform.creatorstudio.model.StudioContent.ContentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing studio content.
 * Handles content association with studios, including uploading,
 * organizing, and featuring content in Studios Special collections.
 */
@RestController
@RequestMapping("/api/v1/studio-content")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Studio Content", description = "APIs for managing studio content")
@SecurityRequirement(name = "Bearer Authentication")
public class StudioContentController {

    private final StudioContentService contentService;

    /**
     * Adds content to a studio.
     *
     * @param actionDTO The content action data
     * @return Response containing the created content association
     */
    @Operation(summary = "Add content to studio", description = "Associates content with a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Content added successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContentDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Content already associated with a studio")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(#actionDTO.studioId)")
    public ResponseEntity<ApiResponse<ContentDTO>> addContent(
            @RequestBody ContentActionDTO actionDTO) {
        var content = contentService.addContent(
                actionDTO.getStudioId(),
                actionDTO.getContentReferenceId(),
                ContentType.valueOf(actionDTO.getContentType()),
                actionDTO.getTitle(),
                actionDTO.getDescription(),
                actionDTO.getContentCategory(),
                actionDTO.getTags(),
                actionDTO.isSpecial());

        ContentDTO contentDTO = new ContentDTO(content);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(contentDTO));
    }

    /**
     * Gets content by its ID.
     *
     * @param contentId The content ID
     * @return Response containing the content
     */
    @Operation(summary = "Get content by ID", description = "Retrieves content by its unique ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Content found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContentDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Content not found")
    })
    @GetMapping("/{contentId}")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canViewContent(@studioContentService.getStudioIdByContentId(#contentId))")
    public ResponseEntity<ApiResponse<ContentDTO>> getContentById(
            @Parameter(description = "Content ID", required = true) @PathVariable UUID contentId) {
        var content = contentService.getContentById(contentId);
        if (content == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Content not found"));
        }

        ContentDTO contentDTO = new ContentDTO(content);
        return ResponseEntity.ok(ApiResponse.success(contentDTO));
    }

    /**
     * Gets all content for a studio.
     *
     * @param studioId The studio ID
     * @return Response containing the list of content
     */
    @Operation(summary = "Get studio content", description = "Retrieves all content associated with a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Studio content retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContentDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Studio not found")
    })
    @GetMapping("/studio/{studioId}")
    @PreAuthorize("@studioMembershipService.canViewContent(#studioId)")
    public ResponseEntity<ApiResponse<List<ContentDTO>>> getStudioContent(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId) {
        List<ContentDTO> contents = contentService.getStudioContent(studioId)
                .stream()
                .map(ContentDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(contents));
    }

    /**
     * Gets published content for a studio.
     *
     * @param studioId The studio ID
     * @return Response containing the list of published content
     */
    @Operation(summary = "Get published studio content", description = "Retrieves published content associated with a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Published studio content retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContentDTO.class)))
    })
    @GetMapping("/studio/{studioId}/published")
    public ResponseEntity<ApiResponse<List<ContentDTO>>> getPublishedStudioContent(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId) {
        List<ContentDTO> contents = contentService.getPublishedStudioContent(studioId)
                .stream()
                .map(ContentDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(contents));
    }

    /**
     * Gets Studios Special content for a studio.
     *
     * @param studioId The studio ID
     * @return Response containing the list of featured content
     */
    @Operation(summary = "Get Studios Special content", description = "Retrieves content featured in the Studios Special collection")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Studios Special content retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContentDTO.class)))
    })
    @GetMapping("/studio/{studioId}/special")
    public ResponseEntity<ApiResponse<List<ContentDTO>>> getStudiosSpecialContent(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId) {
        List<ContentDTO> contents = contentService.getStudiosSpecialContent(studioId)
                .stream()
                .map(ContentDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(contents));
    }

    /**
     * Updates content information.
     *
     * @param contentId The content ID
     * @param actionDTO The content action data
     * @return Response containing the updated content
     */
    @Operation(summary = "Update studio content", description = "Updates information for studio content")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Content updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContentDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Content not found")
    })
    @PutMapping("/{contentId}")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioContentService.getStudioIdByContentId(#contentId))")
    public ResponseEntity<ApiResponse<ContentDTO>> updateContent(
            @Parameter(description = "Content ID", required = true) @PathVariable UUID contentId,
            @RequestBody ContentActionDTO actionDTO) {
        var content = contentService.updateContent(
                contentId,
                actionDTO.getTitle(),
                actionDTO.getDescription(),
                actionDTO.getContentCategory(),
                actionDTO.getTags(),
                actionDTO.isSpecial());

        ContentDTO contentDTO = new ContentDTO(content);
        return ResponseEntity.ok(ApiResponse.success(contentDTO));
    }

    /**
     * Toggles whether content is featured in Studios Special.
     *
     * @param contentId The content ID
     * @return Response indicating success
     */
    @Operation(summary = "Toggle Studios Special content", description = "Toggles content's feature status in Studios Special collection")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Content feature status toggled successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContentDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Content not found")
    })
    @PatchMapping("/{contentId}/special")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioContentService.getStudioIdByContentId(#contentId))")
    public ResponseEntity<ApiResponse<ContentDTO>> toggleSpecialContent(
            @Parameter(description = "Content ID", required = true) @PathVariable UUID contentId) {
        var content = contentService.toggleSpecialContent(contentId);
        ContentDTO contentDTO = new ContentDTO(content);
        return ResponseEntity.ok(ApiResponse.success(contentDTO));
    }

    /**
     * Reorders content in a studio.
     *
     * @param studioId The studio ID
     * @param contentIds The content IDs in the new order
     * @return Response indicating success
     */
    @Operation(summary = "Reorder studio content", description = "Reorders content within a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Content reordered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @PutMapping("/studio/{studioId}/reorder")
    @PreAuthorize("@studioMembershipService.canManageContent(#studioId)")
    public ResponseEntity<ApiResponse<Void>> reorderContent(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId,
            @RequestBody List<UUID> contentIds) {
        contentService.reorderContent(studioId, contentIds);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Removes content from a studio.
     *
     * @param contentId The content ID to remove
     * @return Response indicating success
     */
    @Operation(summary = "Remove content from studio", description = "Removes content association from a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Content removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Content not found")
    })
    @DeleteMapping("/{contentId}")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioContentService.getStudioIdByContentId(#contentId))")
    public ResponseEntity<ApiResponse<Void>> removeContent(
            @Parameter(description = "Content ID", required = true) @PathVariable UUID contentId) {
        contentService.removeContent(contentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Searches content within a studio by title or description.
     *
     * @param studioId The studio ID
     * @param searchTerm The search term
     * @return Response containing the list of matching content
     */
    @Operation(summary = "Search studio content", description = "Searches content within a studio by title or description")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContentDTO.class)))
    })
    @GetMapping("/studio/{studioId}/search")
    @PreAuthorize("@studioMembershipService.canViewContent(#studioId)")
    public ResponseEntity<ApiResponse<List<ContentDTO>>> searchContent(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId,
            @Parameter(description = "Search term", required = true) @RequestParam String searchTerm) {
        List<ContentDTO> contents = contentService.searchContent(studioId, searchTerm)
                .stream()
                .map(ContentDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(contents));
    }
}
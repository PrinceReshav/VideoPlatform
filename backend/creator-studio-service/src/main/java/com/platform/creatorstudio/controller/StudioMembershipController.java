package com.platform.creatorstudio.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.creatorstudio.dto.MembershipDTO;
import com.platform.creatorstudio.dto.MembershipActionDTO;
import com.platform.creatorstudio.service.StudioMembershipService;
import com.platform.creatorstudio.model.StudioMembership.StudioRole;
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
 * Controller for managing studio memberships.
 * Handles user membership in studios, role assignments, and permissions.
 */
@RestController
@RequestMapping("/api/v1/studio-memberships")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Studio Memberships", description = "APIs for managing studio memberships")
@SecurityRequirement(name = "Bearer Authentication")
public class StudioMembershipController {

    private final StudioMembershipService membershipService;

    /**
     * Adds a user as a member of a studio.
     *
     * @param actionDTO The membership action data
     * @return Response containing the created membership
     */
    @Operation(summary = "Add member to studio", description = "Adds a user as a member of a studio with a specific role")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Member added successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MembershipDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User is already a member")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#actionDTO.studioId)")
    public ResponseEntity<ApiResponse<MembershipDTO>> addMember(
            @RequestBody MembershipActionDTO actionDTO) {
        var membership = membershipService.addMember(
                actionDTO.getStudioId(),
                actionDTO.getUserId(),
                StudioRole.valueOf(actionDTO.getRole()));

        MembershipDTO membershipDTO = new MembershipDTO(membership);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(membershipDTO));
    }

    /**
     * Removes a user from a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID to remove
     * @return Response indicating success
     */
    @Operation(summary = "Remove member from studio", description = "Removes a user from a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Member removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Studio not found or user not a member")
    })
    @DeleteMapping("/{studioId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#studioId)")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId,
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        membershipService.removeMember(studioId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Updates a user's role in a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID
     * @param actionDTO The membership action data containing the new role
     * @return Response containing the updated membership
     */
    @Operation(summary = "Update member role", description = "Updates a user's role in a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Member role updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MembershipDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Studio not found or user not a member")
    })
    @PutMapping("/{studioId}/members/{userId}/role")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#studioId)")
    public ResponseEntity<ApiResponse<MembershipDTO>> updateMemberRole(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId,
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
            @RequestBody MembershipActionDTO actionDTO) {
        var membership = membershipService.updateMemberRole(
                studioId,
                userId,
                StudioRole.valueOf(actionDTO.getRole()));

        MembershipDTO membershipDTO = new MembershipDTO(membership);
        return ResponseEntity.ok(ApiResponse.success(membershipDTO));
    }

    /**
     * Gets a user's membership in a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID
     * @return Response containing the membership
     */
    @Operation(summary = "Get user membership in studio", description = "Retrieves a user's membership in a specific studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Membership found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MembershipDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Membership not found")
    })
    @GetMapping("/{studioId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canViewMembers(#studioId)")
    public ResponseEntity<ApiResponse<MembershipDTO>> getMembership(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId,
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        var membership = membershipService.getMembership(studioId, userId);
        if (membership == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Membership not found"));
        }

        MembershipDTO membershipDTO = new MembershipDTO(membership);
        return ResponseEntity.ok(ApiResponse.success(membershipDTO));
    }

    /**
     * Gets all members of a studio.
     *
     * @param studioId The studio ID
     * @return Response containing the list of members
     */
    @Operation(summary = "Get studio members", description = "Retrieves all members of a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Members retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MembershipDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Studio not found")
    })
    @GetMapping("/{studioId}/members")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canViewMembers(#studioId)")
    public ResponseEntity<ApiResponse<List<MembershipDTO>>> getStudioMembers(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId) {
        List<MembershipDTO> memberships = membershipService.getStudioMembers(studioId)
                .stream()
                .map(MembershipDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(memberships));
    }

    /**
     * Gets all studios a user is a member of.
     *
     * @param userId The user ID
     * @return Response containing the list of studio memberships
     */
    @Operation(summary = "Get user's studio memberships", description = "Retrieves all studios a user is a member of")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Memberships retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MembershipDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found or not a member of any studio")
    })
    @GetMapping("/users/{userId}/memberships")
    public ResponseEntity<ApiResponse<List<MembershipDTO>>> getUserMemberships(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        List<MembershipDTO> memberships = membershipService.getUserMemberships(userId)
                .stream()
                .map(MembershipDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(memberships));
    }

    /**
     * Checks if a user is a member of a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID
     * @return Response indicating membership status
     */
    @Operation(summary = "Check user membership", description = "Checks if a user is a member of a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Membership status checked",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class)))
    })
    @GetMapping("/{studioId}/members/{userId}/check")
    public ResponseEntity<ApiResponse<Boolean>> isMember(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId,
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        boolean isMember = membershipService.isMember(studioId, userId);
        return ResponseEntity.ok(ApiResponse.success(isMember));
    }

    /**
     * Gets a user's role in a studio.
     *
     * @param studioId The studio ID
     * @param userId The user ID
     * @return Response containing the user's role
     */
    @Operation(summary = "Get user role in studio", description = "Retrieves a user's role in a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User role retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{studioId}/members/{userId}/role")
    public ResponseEntity<ApiResponse<String>> getUserRole(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId,
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        var role = membershipService.getUserRole(studioId, userId);
        if (role == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("User is not a member of this studio"));
        }
        return ResponseEntity.ok(ApiResponse.success(role.toString()));
    }
}
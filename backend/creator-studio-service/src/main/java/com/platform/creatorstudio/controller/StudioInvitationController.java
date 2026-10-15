package com.platform.creatorstudio.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.creatorstudio.dto.InvitationDTO;
import com.platform.creatorstudio.dto.InvitationActionDTO;
import com.platform.creatorstudio.service.StudioInvitationService;
import com.platform.creatorstudio.model.StudioInvitation.StudioRole;
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
 * Controller for managing studio invitations.
 * Handles sending, accepting, and managing studio invitations.
 */
@RestController
@RequestMapping("/api/v1/studio-invitations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Studio Invitations", description = "APIs for managing studio invitations")
@SecurityRequirement(name = "Bearer Authentication")
public class StudioInvitationController {

    private final StudioInvitationService invitationService;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Sends an invitation to join a studio.
     *
     * @param actionDTO The invitation action data
     * @return Response containing the created invitation
     */
    @Operation(summary = "Send studio invitation", description = "Sends an invitation to join a studio with a specific role")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Invitation sent successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InvitationDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Invitation already sent")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#actionDTO.studioId)")
    public ResponseEntity<ApiResponse<InvitationDTO>> sendInvitation(
            @RequestBody InvitationActionDTO actionDTO) {
        var invitation = invitationService.sendInvitation(
                actionDTO.getStudioId(),
                actionDTO.getEmail(),
                StudioRole.valueOf(actionDTO.getRole()),
                actionDTO.getExpiresInHours());

        InvitationDTO invitationDTO = new InvitationDTO(invitation);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(invitationDTO));
    }

    /**
     * Accepts an invitation to join a studio.
     *
     * @param token The invitation token
     * @param userId The ID of the user accepting the invitation
     * @return Response containing the created membership
     */
    @Operation(summary = "Accept studio invitation", description = "Accepts an invitation to join a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation accepted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation not found or expired"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Invitation already accepted")
    })
    @PostMapping("/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @Parameter(description = "Invitation token", required = true) @RequestParam String token) {
        invitationService.acceptInvitation(token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Revokes an invitation.
     *
     * @param token The invitation token
     * @return Response indicating success
     */
    @Operation(summary = "Revoke studio invitation", description = "Revokes a sent studio invitation")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation revoked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation not found")
    })
    @DeleteMapping("/{token}")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(@studioInvitationService.getStudioIdByToken(#token))")
    public ResponseEntity<ApiResponse<Void>> revokeInvitation(
            @Parameter(description = "Invitation token", required = true) @PathVariable String token) {
        invitationService.revokeInvitation(token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Gets an invitation by its token.
     *
     * @param token The invitation token
     * @return Response containing the invitation
     */
    @Operation(summary = "Get invitation by token", description = "Retrieves an invitation by its unique token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InvitationDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation not found")
    })
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<InvitationDTO>> getInvitationByToken(
            @Parameter(description = "Invitation token", required = true) @PathVariable String token) {
        var invitation = invitationService.getInvitationByToken(token);
        if (invitation == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Invitation not found"));
        }

        InvitationDTO invitationDTO = new InvitationDTO(invitation);
        return ResponseEntity.ok(ApiResponse.success(invitationDTO));
    }

    /**
     * Gets pending invitations for a studio.
     *
     * @param studioId The studio ID
     * @return Response containing the list of pending invitations
     */
    @Operation(summary = "Get studio pending invitations", description = "Retrieves all pending invitations for a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending invitations retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InvitationDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Studio not found")
    })
    @GetMapping("/studio/{studioId}/pending")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageMembers(#studioId)")
    public ResponseEntity<ApiResponse<List<InvitationDTO>>> getPendingInvitations(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId) {
        List<InvitationDTO> invitations = invitationService.getPendingInvitations(studioId)
                .stream()
                .map(InvitationDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    /**
     * Gets pending invitations for an email address.
     *
     * @param email The email address
     * @return Response containing the list of pending invitations
     */
    @Operation(summary = "Get email pending invitations", description = "Retrieves all pending invitations for an email address")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending invitations retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InvitationDTO.class)))
    })
    @GetMapping("/email/{email}/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<InvitationDTO>>> getPendingInvitationsByEmail(
            @Parameter(description = "Email address", required = true) @PathVariable String email) {
        String authenticatedEmail = currentUserAccessor.getCurrentEmail();
        if (authenticatedEmail == null || !authenticatedEmail.equalsIgnoreCase(email.trim())) {
            return ResponseEntity.status(403).body(ApiResponse.error("You may only view invitations addressed to your account."));
        }
        List<InvitationDTO> invitations = invitationService.getPendingInvitationsByEmail(authenticatedEmail)
                .stream()
                .map(InvitationDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    /**
     * Expires old invitations (cleanup).
     *
     * @return Response indicating the number of invitations expired
     */
    @Operation(summary = "Expire old invitations", description = "Expires old studio invitations (cleanup operation)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitations expired successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Integer.class)))
    })
    @PostMapping("/expire-old")
    public ResponseEntity<ApiResponse<Integer>> expireOldInvitations() {
        int count = invitationService.expireOldInvitations();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
package com.platform.creatorstudio.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.creatorstudio.dto.LiveSessionDTO;
import com.platform.creatorstudio.dto.LiveSessionActionDTO;
import com.platform.creatorstudio.service.StudioLiveSessionService;
import com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus;
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
 * Controller for managing studio live sessions.
 * Handles session creation, scheduling, starting, ending, and participant management.
 */
@RestController
@RequestMapping("/api/v1/studio-live-sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Studio Live Sessions", description = "APIs for managing studio live sessions")
@SecurityRequirement(name = "Bearer Authentication")
public class StudioLiveSessionController {

    private final StudioLiveSessionService sessionService;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Creates a new live session.
     *
     * @param actionDTO The live session action data
     * @return Response containing the created live session
     */
    @Operation(summary = "Create live session", description = "Creates a new live session for a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Live session created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(#actionDTO.studioId)")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> createLiveSession(
            @RequestBody LiveSessionActionDTO actionDTO) {
        var session = sessionService.createLiveSession(
                actionDTO.getStudioId(),
                actionDTO.getTitle(),
                actionDTO.getDescription(),
                actionDTO.getSessionCategory(),
                actionDTO.getTopic(),
                actionDTO.getScheduledStartTime(),
                actionDTO.isPublic(),
                actionDTO.isRequiresRegistration(),
                actionDTO.getMaxAttendees(),
                actionDTO.isRequiresPayment(),
                actionDTO.getAttendancePricePaise(),
                actionDTO.isAllowsCommenting());

        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(sessionDTO));
    }

    /**
     * Gets a live session by its ID.
     *
     * @param sessionId The session ID
     * @return Response containing the live session
     */
    @Operation(summary = "Get live session by ID", description = "Retrieves a live session by its unique ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Live session found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found")
    })
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> getLiveSessionById(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId) {
        var session = sessionService.getLiveSessionById(sessionId);
        if (session == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Live session not found"));
        }
        if (!sessionService.canViewSession(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("You do not have access to this session."));
        }

        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.ok(ApiResponse.success(sessionDTO));
    }

    /**
     * Gets a live session by ID and verifies it belongs to a studio.
     *
     * @param sessionId The session ID
     * @param studioId The expected studio ID
     * @return Response containing the live session
     */
    @Operation(summary = "Get live session by ID and studio", description = "Retrieves a live session by its ID and verifies studio ownership")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Live session found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found or does not belong to studio")
    })
    @GetMapping("/{sessionId}/studio/{studioId}")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> getLiveSessionByIdAndStudio(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId,
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId) {
        var session = sessionService.getLiveSessionByIdAndStudio(sessionId, studioId);
        if (session == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Live session not found or does not belong to studio"));
        }
        if (!sessionService.canViewSession(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("You do not have access to this session."));
        }

        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.ok(ApiResponse.success(sessionDTO));
    }

    /**
     * Gets all live sessions for a studio.
     *
     * @param studioId The studio ID
     * @return Response containing the list of live sessions
     */
    @Operation(summary = "Get studio live sessions", description = "Retrieves all live sessions for a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Studio live sessions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Studio not found")
    })
    @GetMapping("/studio/{studioId}")
    @PreAuthorize("@studioMembershipService.canViewContent(#studioId)")
    public ResponseEntity<ApiResponse<List<LiveSessionDTO>>> getStudioLiveSessions(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId) {
        List<LiveSessionDTO> sessions = sessionService.getStudioLiveSessions(studioId)
                .stream()
                .map(LiveSessionDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Gets upcoming live sessions for a studio.
     *
     * @param studioId The studio ID
     * @return Response containing the list of upcoming live sessions
     */
    @Operation(summary = "Get upcoming studio live sessions", description = "Retrieves upcoming live sessions for a studio")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Upcoming studio live sessions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class)))
    })
    @GetMapping("/studio/{studioId}/upcoming")
    @PreAuthorize("@studioMembershipService.canViewContent(#studioId)")
    public ResponseEntity<ApiResponse<List<LiveSessionDTO>>> getUpcomingStudioLiveSessions(
            @Parameter(description = "Studio ID", required = true) @PathVariable UUID studioId) {
        List<LiveSessionDTO> sessions = sessionService.getUpcomingStudioLiveSessions(studioId)
                .stream()
                .map(LiveSessionDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Gets currently live sessions.
     *
     * @return Response containing the list of currently live sessions
     */
    @Operation(summary = "Get currently live sessions", description = "Retrieves all currently live sessions")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Currently live sessions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class)))
    })
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<List<LiveSessionDTO>>> getCurrentlyLiveSessions() {
        List<LiveSessionDTO> sessions = sessionService.getCurrentlyLiveSessions()
                .stream()
                .map(LiveSessionDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Gets recently ended sessions (for replay availability).
     *
     * @param cutoffHours How many hours back to look for ended sessions
     * @return Response containing the list of recently ended sessions
     */
    @Operation(summary = "Get recently ended sessions", description = "Retrieves recently ended sessions for replay availability")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recently ended sessions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class)))
    })
    @GetMapping("/recently-ended")
    public ResponseEntity<ApiResponse<List<LiveSessionDTO>>> getRecentlyEndedSessions(
            @Parameter(description = "Cutoff hours", required = false) @RequestParam(required = false, defaultValue = "24") int cutoffHours) {
        List<LiveSessionDTO> sessions = sessionService.getRecentlyEndedSessions(cutoffHours)
                .stream()
                .map(LiveSessionDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Schedules a live session (changes status from DRAFT to SCHEDULED).
     *
     * @param sessionId The session ID
     * @return Response containing the updated session
     */
    @Operation(summary = "Schedule live session", description = "Schedules a live session for future execution")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Live session scheduled successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found")
    })
    @PutMapping("/{sessionId}/schedule")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> scheduleLiveSession(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId) {
        var session = sessionService.scheduleLiveSession(sessionId);
        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.ok(ApiResponse.success(sessionDTO));
    }

    /**
     * Starts a live session (changes status from SCHEDULED to LIVE).
     *
     * @param sessionId The session ID
     * @return Response containing the updated session
     */
    @Operation(summary = "Start live session", description = "Starts a live session")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Live session started successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found")
    })
    @PutMapping("/{sessionId}/start")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> startLiveSession(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId) {
        var session = sessionService.startLiveSession(sessionId);
        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.ok(ApiResponse.success(sessionDTO));
    }

    /**
     * Ends a live session (changes status from LIVE to ENDED).
     *
     * @param sessionId The session ID
     * @return Response containing the updated session
     */
    @Operation(summary = "End live session", description = "Ends a live session")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Live session ended successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found")
    })
    @PutMapping("/{sessionId}/end")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> endLiveSession(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId) {
        var session = sessionService.endLiveSession(sessionId);
        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.ok(ApiResponse.success(sessionDTO));
    }

    /**
     * Cancels a live session (changes status to CANCELLED).
     *
     * @param sessionId The session ID
     * @return Response containing the updated session
     */
    @Operation(summary = "Cancel live session", description = "Cancels a live session")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Live session cancelled successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found")
    })
    @PutMapping("/{sessionId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> cancelLiveSession(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId) {
        var session = sessionService.cancelLiveSession(sessionId);
        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.ok(ApiResponse.success(sessionDTO));
    }

    /**
     * Marks a live session as completed (changes status from ENDED to COMPLETED).
     *
     * @param sessionId The session ID
     * @return Response containing the updated session
     */
    @Operation(summary = "Complete live session", description = "Marks a live session as completed")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Live session completed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found")
    })
    @PutMapping("/{sessionId}/complete")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> completeLiveSession(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId) {
        var session = sessionService.completeLiveSession(sessionId);
        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.ok(ApiResponse.success(sessionDTO));
    }

    /**
     * Updates live session information.
     *
     * @param sessionId The session ID
     * @param actionDTO The live session action data
     * @return Response containing the updated session
     */
    @Operation(summary = "Update live session", description = "Updates information for a live session")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Live session updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiveSessionDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found")
    })
    @PutMapping("/{sessionId}")
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public ResponseEntity<ApiResponse<LiveSessionDTO>> updateLiveSession(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId,
            @RequestBody LiveSessionActionDTO actionDTO) {
        var session = sessionService.updateLiveSession(
                sessionId,
                actionDTO.getTitle(),
                actionDTO.getDescription(),
                actionDTO.getSessionCategory(),
                actionDTO.getTopic(),
                actionDTO.getScheduledStartTime(),
                actionDTO.isPublic(),
                actionDTO.isRequiresRegistration(),
                actionDTO.getMaxAttendees(),
                actionDTO.isRequiresPayment(),
                actionDTO.getAttendancePricePaise(),
                actionDTO.isAllowsCommenting());

        LiveSessionDTO sessionDTO = new LiveSessionDTO(session);
        return ResponseEntity.ok(ApiResponse.success(sessionDTO));
    }

    /**
     * Joins a user to a live session as an attendee.
     *
     * @param sessionId The session ID
     * @param userId The user ID joining as attendee
     * @return Response indicating success
     */
    @Operation(summary = "Join live session", description = "Joins a user to a live session as an attendee")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User joined live session successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Session at capacity or user not authorized")
    })
    @PostMapping("/{sessionId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> joinLiveSession(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        boolean success = sessionService.joinLiveSession(sessionId, userId);
        if (!success) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error("Unable to join live session"));
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Leaves a live session as an attendee.
     *
     * @param sessionId The session ID
     * @param userId The user ID leaving as attendee
     * @return Response indicating success
     */
    @Operation(summary = "Leave live session", description = "Leaves a user from a live session as an attendee")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User left live session successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Live session not found")
    })
    @PostMapping("/{sessionId}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> leaveLiveSession(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        boolean success = sessionService.leaveLiveSession(sessionId, userId);
        if (!success) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Unable to leave live session"));
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
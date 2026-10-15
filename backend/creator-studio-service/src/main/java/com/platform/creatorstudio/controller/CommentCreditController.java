package com.platform.creatorstudio.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.creatorstudio.dto.CommentDTO;
import com.platform.creatorstudio.dto.CreditActionDTO;
import com.platform.creatorstudio.service.CommentCreditService;
import com.platform.creatorstudio.model.LiveComment;
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
 * Controller for managing comment credits.
 * Handles purchasing credits, consuming credits for comments,
 * and retrieving comment history.
 */
@RestController
@RequestMapping("/api/v1/comment-credits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comment Credits", description = "APIs for managing comment credits and live session comments")
@SecurityRequirement(name = "Bearer Authentication")
public class CommentCreditController {

    private final CommentCreditService creditService;

    /**
     * Adds credits to a user's account (simulates payment purchase).
     *
     * @param actionDTO The credit action data
     * @return Response containing the transaction record
     */
    @Operation(summary = "Add comment credits", description = "Adds credits to a user's comment account (simulates payment)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credits added successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @PostMapping("/add")
    @PreAuthorize("#actionDTO.userId == authentication.principal.userId or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addCredits(
            @RequestBody CreditActionDTO actionDTO) {
        creditService.addCredits(
                actionDTO.getUserId(),
                actionDTO.getAmountPaise(),
                actionDTO.getDescription());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Gets a user's comment credit account.
     *
     * @param userId The user ID
     * @return Response containing the credit account information
     */
    @Operation(summary = "Get credit account", description = "Retrieves a user's comment credit account information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credit account retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/account/{userId}")
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentDTO>> getCreditAccount(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        long balance = creditService.getCreditBalance(userId);
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setBalancePaise(balance);
        commentDTO.setBalanceRupees(balance / 100.0);
        return ResponseEntity.ok(ApiResponse.success(commentDTO));
    }

    /**
     * Gets a user's comment credit transactions.
     *
     * @param userId The user ID
     * @return Response containing the list of transactions
     */
    @Operation(summary = "Get credit transactions", description = "Retrieves a user's comment credit transaction history")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credit transactions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/transactions/{userId}")
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Object>>> getCreditTransactions(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        List<Object> transactions = creditService.getCreditTransactions(userId)
                .stream().map(transaction -> (Object) transaction).toList();
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    /**
     * Posts a comment to a live session (consumes credits).
     *
     * @param actionDTO The comment action data
     * @return Response containing the posted comment
     */
    @Operation(summary = "Post comment to live session", description = "Posts a comment to a live session (consumes credits)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment posted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CommentDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "Payment required - insufficient credits"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "KYC verification required")
    })
    @PostMapping("/comment")
    @PreAuthorize("#actionDTO.userId == authentication.principal.userId or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentDTO>> postComment(
            @RequestBody CreditActionDTO actionDTO) {
        UUID sessionId = actionDTO.getSessionId();
        if (sessionId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("sessionId is required."));
        }

        var comment = creditService.postComment(
                actionDTO.getUserId(),
                actionDTO.getCommentText(),
                sessionId);

        CommentDTO commentDTO = new CommentDTO(comment);
        return ResponseEntity.ok(ApiResponse.success(commentDTO));
    }

    /**
     * Gets recent comments for a live session.
     *
     * @param sessionId The session ID
     * @param limit Maximum number of comments to return
     * @return Response containing the list of recent comments
     */
    @Operation(summary = "Get recent comments", description = "Retrieves recent comments for a live session")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recent comments retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CommentDTO.class)))
    })
    @GetMapping("/comments/{sessionId}")
    public ResponseEntity<ApiResponse<List<CommentDTO>>> getRecentComments(
            @Parameter(description = "Session ID", required = true) @PathVariable UUID sessionId,
            @Parameter(description = "Limit", required = false) @RequestParam(required = false, defaultValue = "50") int limit) {
        List<CommentDTO> comments = creditService.getRecentComments(sessionId, limit)
                .stream().map(CommentDTO::new).toList();
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * Checks if a user has KYC verification.
     *
     * @param userId The user ID
     * @return Response indicating KYC verification status
     */
    @Operation(summary = "Check KYC verification", description = "Checks if a user has completed KYC verification")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC verification status retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class)))
    })
    @GetMapping("/kyc/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> checkKycVerification(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        boolean hasKyc = creditService.hasKycVerification(userId);
        return ResponseEntity.ok(ApiResponse.success(hasKyc));
    }
}
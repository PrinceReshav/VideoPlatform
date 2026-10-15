package com.platform.user.service;

import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.user.entity.UserRelationship;
import com.platform.user.repository.UserRelationshipRepository;
import com.platform.user.dto.UserRelationshipRequest;
import com.platform.user.dto.UserRelationshipResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling user relationships (Feature 26: Block/Mute/Restrict)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRelationshipService {

    private final UserRelationshipRepository userRelationshipRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * Create a user relationship (block, mute, restrict)
     */
    @Transactional
    public UserRelationshipResponse createUserRelationship(UserRelationshipRequest request) {
        UUID fromUserId = currentUserAccessor.getCurrentUserId();
        UUID toUserId = request.getTargetUserId();

        // Users cannot block/mute/restrict themselves
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST, "Cannot perform relationship action on yourself");
        }

        // Verify target user exists
        // Auth owns the users table. This service only needs an existence check;
        // it must not import or map the Auth Service's JPA entity.
        Integer matchingUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_schema.users WHERE id = ? AND deleted = FALSE",
                Integer.class,
                toUserId
        );
        if (matchingUsers == null || matchingUsers == 0) {
            throw new NotFoundException(ApiErrorCodes.USER_NOT_FOUND, "Target user not found");
        }

        // Check if relationship already exists
        if (userRelationshipRepository.findByFromUserIdAndToUserIdAndRelationshipType(
                fromUserId, toUserId, request.getRelationshipType()).isPresent()) {
            throw new BusinessException(ApiErrorCodes.BAD_REQUEST,
                    "Relationship already exists with this user");
        }

        UserRelationship relationship = new UserRelationship();
        relationship.setFromUserId(fromUserId);
        relationship.setToUserId(toUserId);
        relationship.setRelationshipType(request.getRelationshipType());
        relationship.setNotes(request.getNotes());
        relationship.setCreatedAt(Instant.now());
        relationship.setActive(true);

        UserRelationship saved = userRelationshipRepository.save(relationship);
        log.info("Created {} relationship from user {} to user {}",
                request.getRelationshipType(), fromUserId, toUserId);

        return mapToResponseDTO(saved);
    }

    /**
     * Remove a user relationship (unblock, unmute, unrestrict)
     */
    @Transactional
    public void removeUserRelationship(UUID targetUserId, String relationshipType) {
        UUID fromUserId = currentUserAccessor.getCurrentUserId();

        UserRelationship relationship = userRelationshipRepository.findByFromUserIdAndToUserIdAndRelationshipType(
                fromUserId, targetUserId, relationshipType)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.USER_NOT_FOUND,
                        "Relationship not found"));

        // Soft delete by setting active to false
        relationship.setActive(false);
        relationship.setUpdatedAt(Instant.now());
        userRelationshipRepository.save(relationship);

        log.info("Removed {} relationship from user {} to user {}",
                relationshipType, fromUserId, targetUserId);
    }

    /**
     * Get relationships where the current user is the initiator (blocker, muter, etc.)
     */
    @Transactional(readOnly = true)
    public List<UserRelationshipResponse> getInitiatedRelationships(String relationshipType, int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        // For simplicity, we'll get all without pagination in this example
        // In a real implementation, you'd want to add pagination support
        List<UserRelationship> relationships;
        if (relationshipType == null || relationshipType.isEmpty()) {
            relationships = userRelationshipRepository.findByFromUserId(userId);
        } else {
            relationships = userRelationshipRepository.findByFromUserIdAndRelationshipType(userId, relationshipType);
        }
        return relationships.stream()
                .filter(UserRelationship::isActive)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get relationships where the current user is the target (blocked, muted, etc.)
     */
    @Transactional(readOnly = true)
    public List<UserRelationshipResponse> getReceivedRelationships(String relationshipType, int page, int size) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        // For simplicity, we'll get all without pagination in this example
        // In a real implementation, you'd want to add pagination support
        List<UserRelationship> relationships;
        if (relationshipType == null || relationshipType.isEmpty()) {
            relationships = userRelationshipRepository.findByToUserId(userId);
        } else {
            relationships = userRelationshipRepository.findByToUserIdAndRelationshipType(userId, relationshipType);
        }
        return relationships.stream()
                .filter(UserRelationship::isActive)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if a relationship exists between the current user and target user
     */
    @Transactional(readOnly = true)
    public boolean hasRelationship(UUID targetUserId, String relationshipType) {
        UUID userId = currentUserAccessor.getCurrentUserId();
        return userRelationshipRepository.findByFromUserIdAndToUserIdAndRelationshipType(
                userId, targetUserId, relationshipType)
                .map(UserRelationship::isActive)
                .orElse(false);
    }

    /**
     * Convert UserRelationship entity to UserRelationshipResponse DTO
     */
    private UserRelationshipResponse mapToResponseDTO(UserRelationship relationship) {
        return UserRelationshipResponse.builder()
                .id(relationship.getId())
                .fromUserId(relationship.getFromUserId())
                .toUserId(relationship.getToUserId())
                .relationshipType(relationship.getRelationshipType())
                .notes(relationship.getNotes())
                .active(relationship.isActive())
                .createdAt(relationship.getCreatedAt())
                .updatedAt(relationship.getUpdatedAt())
                .build();
    }

    /**
     * Convert list of UserRelationship entities to list of UserRelationshipResponse DTOs
     */
    private List<UserRelationshipResponse> mapToResponseDTOList(List<UserRelationship> relationships) {
        return relationships.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
}
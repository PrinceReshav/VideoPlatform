package com.platform.creatorstudio.service;

import com.platform.creatorstudio.model.StudioMembership;
import com.platform.creatorstudio.model.StudioMembership.StudioRole;
import com.platform.creatorstudio.repository.StudioMembershipRepository;
import com.platform.creatorstudio.repository.CreatorStudioRepository;
import com.platform.common.security.CurrentUserAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for StudioMembershipService.
 * Tests permission checking methods used in SpEL expressions.
 */
@ExtendWith(MockitoExtension.class)
class StudioMembershipServiceTest {

    @Mock
    private StudioMembershipRepository membershipRepository;

    @Mock
    private CreatorStudioRepository studioRepository;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private StudioMembershipService studioMembershipService;

    private UUID testUserId;
    private UUID testStudioId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testStudioId = UUID.randomUUID();

        // Mock current user
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
    }

    @Test
    void testCanManageMembers_AdminUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(true);

        // Act
        boolean canManage = studioMembershipService.canManageMembers(testStudioId);

        // Assert
        assertTrue(canManage, "Admin users should be able to manage members");
        verify(currentUserAccessor, times(1)).hasRole("ADMIN");
        verify(currentUserAccessor, never()).getCurrentUserId(); // Should not need to check user role
    }

    @Test
    void testCanManageMembers_OwnerUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(false);
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(studioRepository.findById(testStudioId)).thenReturn(Optional.empty()); // Not used in this method
        when(membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId))
                .thenReturn(Optional.of(StudioRole.OWNER));

        // Act
        boolean canManage = studioMembershipService.canManageMembers(testStudioId);

        // Assert
        assertTrue(canManage, "Owners should be able to manage members");
        verify(currentUserAccessor, times(1)).hasRole("ADMIN");
        verify(currentUserAccessor, times(1)).getCurrentUserId();
        verify(membershipRepository, times(1))
                .findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId);
    }

    @Test
    void testCanManageMembers_AdminRoleUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(false);
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId))
                .thenReturn(Optional.of(StudioRole.ADMIN));

        // Act
        boolean canManage = studioMembershipService.canManageMembers(testStudioId);

        // Assert
        assertTrue(canManage, "Admins should be able to manage members");
    }

    @Test
    void testCanManageMembers_NonAuthorizedUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(false);
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId))
                .thenReturn(Optional.of(StudioRole.MEMBER)); // Not owner or admin

        // Act
        boolean canManage = studioMembershipService.canManageMembers(testStudioId);

        // Assert
        assertFalse(canManage, "Regular members should not be able to manage members");
    }

    @Test
    void testCanManageMembers_NoUser() {
        // Arrange
        when(currentUserAccessor.getCurrentUserId()).thenReturn(null);

        // Act
        boolean canManage = studioMembershipService.canManageMembers(testStudioId);

        // Assert
        assertFalse(canManage, "When no user is available, should not be able to manage members");
    }

    @Test
    void testCanManageContent_OwnerUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(false);
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId))
                .thenReturn(Optional.of(StudioRole.OWNER));

        // Act
        boolean canManage = studioMembershipService.canManageContent(testStudioId);

        // Assert
        assertTrue(canManage, "Owners should be able to manage content");
    }

    @Test
    void testCanManageContent_CreatorUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(false);
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId))
                .thenReturn(Optional.of(StudioRole.CREATOR));

        // Act
        boolean canManage = studioMembershipService.canManageContent(testStudioId);

        // Assert
        assertTrue(canManage, "Creators should be able to manage content");
    }

    @Test
    void testCanManageContent_ModeratorUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(false);
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId))
                .thenReturn(Optional.of(StudioRole.MODERATOR));

        // Act
        boolean canManage = studioMembershipService.canManageContent(testStudioId);

        // Assert
        assertFalse(canManage, "Moderators should not be able to manage content (based on current implementation)");
        // Note: According to StudioMembership entity, MODERATOR cannot manage content
    }

    @Test
    void testCanViewContent_AnyMember() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(false);
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId))
                .thenReturn(Optional.of(StudioRole.MEMBER)); // Any role

        // Act
        boolean canView = studioMembershipService.canViewContent(testStudioId);

        // Assert
        assertTrue(canView, "Any studio member should be able to view content");
    }

    @Test
    void testCanViewContent_NoUser() {
        // Arrange
        when(currentUserAccessor.getCurrentUserId()).thenReturn(null);

        // Act
        boolean canView = studioMembershipService.canViewContent(testStudioId);

        // Assert
        assertFalse(canView, "When no user is available, should not be able to view content");
    }

    @Test
    void testCanViewMembers_AdminUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(true);

        // Act
        boolean canView = studioMembershipService.canViewMembers(testStudioId);

        // Assert
        assertTrue(canView, "Admin users should be able to view members");
        verify(currentUserAccessor, times(1)).hasRole("ADMIN");
    }

    @Test
    void testCanViewMembers_MemberUser() {
        // Arrange
        when(currentUserAccessor.hasRole("ADMIN")).thenReturn(false);
        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(membershipRepository.findRoleByStudioIdAndUserIdAndIsActiveTrue(testStudioId, testUserId))
                .thenReturn(Optional.of(StudioRole.MEMBER));

        // Act
        boolean canView = studioMembershipService.canViewMembers(testStudioId);

        // Assert
        assertTrue(canView, "Members should be able to view members");
    }

    @Test
    void testCanViewMembers_NoUser() {
        // Arrange
        when(currentUserAccessor.getCurrentUserId()).thenReturn(null);

        // Act
        boolean canView = studioMembershipService.canViewMembers(testStudioId);

        // Assert
        assertFalse(canView, "When no user is available, should not be able to view members");
    }
}
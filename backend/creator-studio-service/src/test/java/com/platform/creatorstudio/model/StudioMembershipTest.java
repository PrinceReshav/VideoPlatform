package com.platform.creatorstudio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for StudioMembership model.
 * Tests role-based permission methods.
 */
class StudioMembershipTest {

    private StudioMembership membership;

    @BeforeEach
    void setUp() {
        membership = new StudioMembership();
    }

    @Test
    void testIsOwner() {
        // Arrange & Act
        membership.setRole(StudioMembership.StudioRole.OWNER);

        // Assert
        assertTrue(membership.isOwner(), "Should return true for OWNER role");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.ADMIN);

        // Assert
        assertFalse(membership.isOwner(), "Should return false for ADMIN role");
    }

    @Test
    void testIsAdminOrOwner() {
        // Arrange & Act
        membership.setRole(StudioMembership.StudioRole.OWNER);
        assertTrue(membership.isAdminOrOwner(), "Should return true for OWNER");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.ADMIN);
        assertTrue(membership.isAdminOrOwner(), "Should return true for ADMIN");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MANAGER);
        assertFalse(membership.isAdminOrOwner(), "Should return false for MANAGER");
    }

    @Test
    void testCanManageMembers() {
        // Arrange & Act
        membership.setRole(StudioMembership.StudioRole.OWNER);
        assertTrue(membership.canManageMembers(), "OWNER should be able to manage members");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.ADMIN);
        assertTrue(membership.canManageMembers(), "ADMIN should be able to manage members");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MANAGER);
        assertFalse(membership.canManageMembers(), "MANAGER should not be able to manage members");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MODERATOR);
        assertFalse(membership.canManageMembers(), "MODERATOR should not be able to manage members");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.CREATOR);
        assertFalse(membership.canManageMembers(), "CREATOR should not be able to manage members");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MEMBER);
        assertFalse(membership.canManageMembers(), "MEMBER should not be able to manage members");
    }

    @Test
    void testCanManageContent() {
        // Arrange & Act
        membership.setRole(StudioMembership.StudioRole.OWNER);
        assertTrue(membership.canManageContent(), "OWNER should be able to manage content");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.ADMIN);
        assertTrue(membership.canManageContent(), "ADMIN should be able to manage content");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MANAGER);
        assertTrue(membership.canManageContent(), "MANAGER should be able to manage content");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.CREATOR);
        assertTrue(membership.canManageContent(), "CREATOR should be able to manage content");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MODERATOR);
        assertFalse(membership.canManageContent(), "MODERATOR should not be able to manage content");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MEMBER);
        assertFalse(membership.canManageContent(), "MEMBER should not be able to manage content");
    }

    @Test
    void testCanModerate() {
        // Arrange & Act
        membership.setRole(StudioMembership.StudioRole.OWNER);
        assertTrue(membership.canModerate(), "OWNER should be able to moderate");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.ADMIN);
        assertTrue(membership.canModerate(), "ADMIN should be able to moderate");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MANAGER);
        assertTrue(membership.canModerate(), "MANAGER should be able to moderate");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MODERATOR);
        assertTrue(membership.canModerate(), "MODERATOR should be able to moderate");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.CREATOR);
        assertFalse(membership.canModerate(), "CREATOR should not be able to moderate");

        // Arrange
        membership.setRole(StudioMembership.StudioRole.MEMBER);
        assertFalse(membership.canModerate(), "MEMBER should not be able to moderate");
    }

    @Test
    void testIsActiveMember() {
        // Arrange & Act
        membership.setActive(true);
        assertTrue(membership.isActiveMember(), "Should return true when active");

        // Arrange
        membership.setActive(false);
        assertFalse(membership.isActiveMember(), "Should return false when not active");
    }
}
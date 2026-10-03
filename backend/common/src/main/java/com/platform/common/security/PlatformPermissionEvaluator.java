package com.platform.common.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

@Component
public class PlatformPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof PlatformUserDetails userDetails)) {
            return false;
        }

        String perm = permission.toString();

        // Check role-based permissions
        if (perm.startsWith("ROLE_")) {
            return userDetails.getRoles().stream()
                    .anyMatch(role -> role.getAuthority().equals(perm));
        }

        // Check ownership permissions
        if (perm.equals("OWNER") && targetDomainObject instanceof Ownable ownable) {
            return ownable.getOwnerId().equals(userDetails.getUserId());
        }

        // Admin has all permissions
        if (userDetails.getRoles().contains(PlatformUserDetails.Role.ADMIN)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        // For ID-based permission checks, we'd need to load the entity
        // This is a fallback - prefer hasPermission with loaded domain object
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof PlatformUserDetails userDetails)) {
            return false;
        }

        String perm = permission.toString();

        if (perm.startsWith("ROLE_")) {
            return userDetails.getRoles().stream()
                    .anyMatch(role -> role.getAuthority().equals(perm));
        }

        if (userDetails.getRoles().contains(PlatformUserDetails.Role.ADMIN)) {
            return true;
        }

        return false;
    }

    public interface Ownable {
        UUID getOwnerId();
    }
}
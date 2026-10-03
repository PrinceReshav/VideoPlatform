package com.platform.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public class CurrentUserAccessor {

    public Optional<PlatformUserDetails> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof PlatformUserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    public UUID getCurrentUserId() {
        return getCurrentUser()
                .map(PlatformUserDetails::getUserId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public String getCurrentUsername() {
        return getCurrentUser()
                .map(PlatformUserDetails::getUsername)
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public String getCurrentEmail() {
        return getCurrentUser()
                .map(PlatformUserDetails::getEmail)
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public boolean hasRole(PlatformUserDetails.Role role) {
        return getCurrentUser()
                .map(u -> u.getRoles().contains(role))
                .orElse(false);
    }

    public boolean isAdmin() {
        return hasRole(PlatformUserDetails.Role.ADMIN);
    }

    public boolean isCreator() {
        return hasRole(PlatformUserDetails.Role.CREATOR);
    }
}
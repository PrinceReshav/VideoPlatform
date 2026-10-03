package com.platform.common.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AuditListener {

    @PrePersist
    public void onPrePersist(Auditable entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.Instant.now());
        }
        entity.setUpdatedAt(java.time.Instant.now());

        Optional<UUID> currentUserId = getCurrentUserId();
        currentUserId.ifPresent(entity::setCreatedBy);
    }

    @PreUpdate
    public void onPreUpdate(Auditable entity) {
        entity.setUpdatedAt(java.time.Instant.now());

        Optional<UUID> currentUserId = getCurrentUserId();
        currentUserId.ifPresent(entity::setUpdatedBy);
    }

    private Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof com.platform.common.security.PlatformUserDetails userDetails) {
            return Optional.of(userDetails.getUserId());
        }
        return Optional.empty();
    }
}
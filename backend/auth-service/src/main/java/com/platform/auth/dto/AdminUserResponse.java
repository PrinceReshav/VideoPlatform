package com.platform.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdminUserResponse {
    private UUID id;
    private String email;
    private String username;
    private String displayName;
    private String avatarUrl;
    private List<String> roles;
    private boolean blocked;
    private Instant lockedUntil;
    private boolean emailVerified;
    private Instant createdAt;
}

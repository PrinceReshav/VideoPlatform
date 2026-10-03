package com.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private Instant issuedAt;
    private UUID userId;
    private String username;
    private String email;
    private Set<String> roles;

    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn,
                                   UUID userId, String username, String email, Set<String> roles) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .issuedAt(Instant.now())
                .userId(userId)
                .username(username)
                .email(email)
                .roles(roles)
                .build();
    }
}
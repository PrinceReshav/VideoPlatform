package com.platform.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PlatformUserDetails implements UserDetails {
    private final UUID userId;
    private final String email;
    private final String username;
    private final String password;
    private final List<Role> roles;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;

    public PlatformUserDetails(UUID userId, String email, String username, String password,
                               List<Role> roles, boolean enabled, boolean accountNonExpired,
                               boolean accountNonLocked, boolean credentialsNonExpired) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<Role> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public enum Role implements GrantedAuthority {
        USER("ROLE_USER"),
        CREATOR("ROLE_CREATOR"),
        ADMIN("ROLE_ADMIN"),
        MODERATOR("ROLE_MODERATOR"),
        PRODUCTION_HOUSE("ROLE_PRODUCTION_HOUSE");

        private final String authority;

        Role(String authority) {
            this.authority = authority;
        }

        @Override
        public String getAuthority() {
            return authority;
        }

        public static Role fromAuthority(String authority) {
            for (Role role : values()) {
                if (role.authority.equals(authority)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unknown role: " + authority);
        }
    }
}
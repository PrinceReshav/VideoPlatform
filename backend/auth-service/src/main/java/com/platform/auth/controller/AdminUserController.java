package com.platform.auth.controller;

import com.platform.auth.dto.AdminUserResponse;
import com.platform.auth.entity.User;
import com.platform.auth.repository.UserRepository;
import com.platform.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserRepository repository;

    @GetMapping
    public ResponseEntity<?> getUsers(@RequestParam(required=false) String search,
                                      @RequestParam(required=false) String role,
                                      @RequestParam(required=false) String status,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="20") int size) {
        List<User> all = repository.findAll();
        String q = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();
        List<User> filtered = all.stream().filter(u -> {
            boolean matchesSearch = q.isBlank() || u.getEmail().toLowerCase(Locale.ROOT).contains(q)
                    || u.getUsername().toLowerCase(Locale.ROOT).contains(q)
                    || (u.getDisplayName() != null && u.getDisplayName().toLowerCase(Locale.ROOT).contains(q));
            boolean matchesRole = role == null || role.isBlank() || u.getRoles().stream().anyMatch(r -> r.name().equalsIgnoreCase(role));
            boolean blocked = u.isLocked();
            boolean matchesStatus = status == null || status.isBlank() || (status.equalsIgnoreCase("blocked") ? blocked : status.equalsIgnoreCase("active") ? !blocked : true);
            return matchesSearch && matchesRole && matchesStatus;
        }).toList();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<AdminUserResponse> content = filtered.subList(from, to).stream().map(this::map).toList();
        return ResponseEntity.ok(PageResponse.of(content, page, size, filtered.size()));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<Void> ban(@PathVariable UUID id, @RequestBody(required=false) java.util.Map<String,String> body) {
        User user = repository.findById(id).orElseThrow();
        user.setLockedUntil(Instant.now().plusSeconds(365L * 24 * 60 * 60));
        repository.save(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<Void> unban(@PathVariable UUID id) {
        User user = repository.findById(id).orElseThrow();
        user.setLockedUntil(null); user.setFailedLoginAttempts(0); repository.save(user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Void> updateRole(@PathVariable UUID id, @RequestBody java.util.Map<String,String> body) {
        User user = repository.findById(id).orElseThrow();
        user.getRoles().clear();
        user.addRole(User.Role.valueOf(body.getOrDefault("role", "USER").toUpperCase(Locale.ROOT)));
        repository.save(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = repository.findById(id).orElseThrow();
        user.markDeleted(UUID.randomUUID());
        repository.save(user);
        return ResponseEntity.noContent().build();
    }

    private AdminUserResponse map(User u) {
        return AdminUserResponse.builder().id(u.getId()).email(u.getEmail()).username(u.getUsername())
                .displayName(u.getDisplayName()).avatarUrl(u.getAvatarUrl())
                .roles(u.getRoles().stream().map(User.Role::name).toList()).blocked(u.isLocked())
                .lockedUntil(u.getLockedUntil()).emailVerified(u.isEmailVerified()).createdAt(u.getCreatedAt()).build();
    }
}

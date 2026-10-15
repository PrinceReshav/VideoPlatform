package com.platform.auth.config;

import com.platform.auth.entity.User;
import com.platform.auth.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Configuration
@Profile("dev")
public class DevDataSeeder {
    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Bean
    org.springframework.boot.CommandLineRunner seedUsers(UserRepository repository, PasswordEncoder encoder) {
        return args -> {
            seed(repository, encoder, ADMIN_ID, "admin@videoplatform.local", "admin", "Platform Admin", "Admin@123", User.Role.ADMIN);
            seed(repository, encoder, CREATOR_ID, "creator@videoplatform.local", "creator", "Demo Creator", "Creator@123", User.Role.CREATOR);
            seed(repository, encoder, USER_ID, "user@videoplatform.local", "viewer", "Demo Viewer", "Viewer@123", User.Role.USER);
        };
    }

    private void seed(UserRepository repository, PasswordEncoder encoder, UUID id, String email,
                      String username, String displayName, String password, User.Role role) {
        // The shared development database may already contain users created by an
        // earlier run with different UUIDs. Never attempt a duplicate email or username
        // insert, and never overwrite an existing account's credentials or roles.
        if (repository.existsById(id)
                || repository.existsByEmail(email)
                || repository.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(encoder.encode(password));
        user.setEmailVerified(true);
        user.setLocale("en");
        user.setTimezone("Asia/Kolkata");
        user.addRole(role);
        repository.save(user);
    }
}

package com.platform.auth.service;

import com.platform.auth.dto.ForgotPasswordRequest;
import com.platform.auth.dto.LoginRequest;
import com.platform.auth.dto.RegisterRequest;
import com.platform.auth.dto.ResetPasswordRequest;
import com.platform.auth.dto.TokenResponse;
import com.platform.auth.entity.RefreshToken;
import com.platform.auth.entity.User;
import com.platform.common.event.NotificationRequestedEvent;
import com.platform.common.notification.NotificationType;
import com.platform.auth.repository.RefreshTokenRepository;
import com.platform.auth.repository.UserRepository;
import com.platform.auth.security.JwtProvider;
import com.platform.common.dto.ApiErrorCodes;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ConflictException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.ValidationException;
import com.platform.common.kafka.EventEnvelope;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.util.ChecksumUtil;
import com.platform.common.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${auth.require-email-verification:true}")
    private boolean requireEmailVerification;

    @Transactional
    public TokenResponse register(RegisterRequest request, String ipAddress) {
        // Validate
        ValidationUtil.validateAndThrow(request);

        if (!ValidationUtil.isValidEmail(request.getEmail())) {
            throw new ValidationException("Invalid email format", java.util.Map.of("email", "Invalid email format"));
        }

        if (!ValidationUtil.isValidUsername(request.getUsername())) {
            throw new ValidationException("Invalid username format", java.util.Map.of("username", "Invalid username format"));
        }

        if (!ValidationUtil.isStrongPassword(request.getPassword())) {
            throw new ValidationException("Password must contain uppercase, lowercase, digit, and special character",
                    java.util.Map.of("password", "Password too weak"));
        }

        // Check uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(ApiErrorCodes.EMAIL_ALREADY_REGISTERED, "Email already registered");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException(ApiErrorCodes.USERNAME_ALREADY_TAKEN, "Username already taken");
        }

        // Create user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.getEmail().toLowerCase());
        user.setUsername(request.getUsername().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setLocale(request.getLocale());
        user.setTimezone(request.getTimezone());
        user.addRole(User.Role.USER);
        user.setEmailVerified(false);

        // Generate email verification token
        String verificationToken = jwtProvider.generateEmailVerificationToken(user.getId());
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiresAt(Instant.now().plusSeconds(24 * 60 * 60));

        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        saveRefreshToken(user.getId(), refreshToken, ipAddress, request.getDeviceInfo());

        // Publish event
        publishUserRegisteredEvent(user);

        log.info("User registered: {}", user.getEmail());
        return TokenResponse.of(accessToken, refreshToken, jwtProvider.getAccessTokenExpiryMs(),
                user.getId(), user.getUsername(), user.getEmail(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenResponse login(LoginRequest request, String ipAddress) {
        ValidationUtil.validateAndThrow(request);

        String emailOrUsername = request.getEmailOrUsername().toLowerCase();
        User user = userRepository.findByEmail(emailOrUsername)
                .or(() -> userRepository.findByUsername(emailOrUsername))
                .orElseThrow(() -> new BusinessException(ApiErrorCodes.INVALID_CREDENTIALS,
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.isLocked()) {
            throw new BusinessException(ApiErrorCodes.ACCOUNT_LOCKED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Account temporarily locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        if (requireEmailVerification && !user.isEmailVerified()) {
            throw new BusinessException(ApiErrorCodes.EMAIL_NOT_VERIFIED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Email not verified");
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        saveRefreshToken(user.getId(), refreshToken, ipAddress, request.getDeviceInfo());

        log.info("User logged in: {}", user.getEmail());
        return TokenResponse.of(accessToken, refreshToken, jwtProvider.getAccessTokenExpiryMs(),
                user.getId(), user.getUsername(), user.getEmail(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }

    @Transactional
    public TokenResponse refreshToken(String refreshToken, String ipAddress) {
        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ApiErrorCodes.TOKEN_INVALID,
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        UUID userId = jwtProvider.getUserIdFromToken(refreshToken);
        String tokenHash = ChecksumUtil.sha256(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ApiErrorCodes.REFRESH_TOKEN_REVOKED,
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Refresh token revoked"));

        if (!storedToken.isValid()) {
            throw new BusinessException(ApiErrorCodes.REFRESH_TOKEN_EXPIRED,
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.USER_NOT_FOUND, "User not found"));

        if (user.isLocked()) {
            throw new BusinessException(ApiErrorCodes.ACCOUNT_LOCKED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "Account temporarily locked");
        }

        // Rotate refresh token
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);
        String newTokenHash = ChecksumUtil.sha256(newRefreshToken);
        refreshTokenRepository.revokeAndReplace(tokenHash, newTokenHash, Instant.now());

        String accessToken = jwtProvider.generateAccessToken(user);

        log.debug("Token refreshed for user: {}", user.getEmail());
        return TokenResponse.of(accessToken, newRefreshToken, jwtProvider.getAccessTokenExpiryMs(),
                user.getId(), user.getUsername(), user.getEmail(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && jwtProvider.validateToken(refreshToken) && jwtProvider.isRefreshToken(refreshToken)) {
            String tokenHash = ChecksumUtil.sha256(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        token.revoke(null);
                        refreshTokenRepository.save(token);
                    });
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        ValidationUtil.validateAndThrow(request);

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElse(null); // Don't reveal if email exists

        if (user != null && user.isEmailVerified()) {
            String resetToken = jwtProvider.generatePasswordResetToken(user.getId());
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetExpiresAt(Instant.now().plusSeconds(60 * 60));
            userRepository.save(user);

            // Email delivery is intentionally infrastructure-specific; the token is persisted and logged at debug/info level for DEV.
            log.debug("Password reset token generated for user: {}", user.getEmail());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        ValidationUtil.validateAndThrow(request);

        if (!ValidationUtil.isStrongPassword(request.getNewPassword())) {
            throw new ValidationException("Password must contain uppercase, lowercase, digit, and special character",
                    java.util.Map.of("newPassword", "Password too weak"));
        }

        UUID userId;
        try {
            userId = jwtProvider.getUserIdFromToken(request.getToken());
        } catch (Exception e) {
            throw new BusinessException(ApiErrorCodes.TOKEN_INVALID,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid reset token");
        }

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.USER_NOT_FOUND, "User not found"));

        if (user.getPasswordResetToken() == null ||
                !user.getPasswordResetToken().equals(request.getToken()) ||
                user.getPasswordResetExpiresAt() == null ||
                user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ApiErrorCodes.TOKEN_EXPIRED,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Reset token expired or invalid");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());

        userRepository.save(user);
        log.info("Password reset for user: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {
        UUID userId;
        try {
            userId = jwtProvider.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new BusinessException(ApiErrorCodes.TOKEN_INVALID,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid verification token");
        }

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorCodes.USER_NOT_FOUND, "User not found"));

        if (user.getEmailVerificationToken() == null ||
                !user.getEmailVerificationToken().equals(token) ||
                user.getEmailVerificationExpiresAt() == null ||
                user.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ApiErrorCodes.TOKEN_EXPIRED,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Verification token expired or invalid");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    private void saveRefreshToken(UUID userId, String refreshToken, String ipAddress, String deviceInfo) {
        String tokenHash = ChecksumUtil.sha256(refreshToken);
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(tokenHash);
        token.setDeviceInfo(deviceInfo);
        token.setIpAddress(ipAddress);
        token.setExpiresAt(Instant.now().plusMillis(jwtProvider.getRefreshTokenExpiryMs()));
        refreshTokenRepository.save(token);
    }

    private void publishUserRegisteredEvent(User user) {
        NotificationRequestedEvent notification = NotificationRequestedEvent.builder()
                .notificationId(UUID.randomUUID())
                .userId(user.getId())
                .type(NotificationType.SYSTEM_ANNOUNCEMENT)
                .title("Welcome to VideoPlatform")
                .message("Your account is ready. Explore videos, creators and channels.")
                .priority(1)
                .timestamp(Instant.now())
                .build();

        EventEnvelope<NotificationRequestedEvent> envelope = EventEnvelope.create(
                "notification.requested", "v1", notification, "auth-service"
        );

        kafkaTemplate.send(KafkaTopicNames.NOTIFICATION_REQUESTED, user.getId().toString(), envelope);
    }
}
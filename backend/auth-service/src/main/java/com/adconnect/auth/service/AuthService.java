package com.adconnect.auth.service;

import com.adconnect.auth.config.AuthProperties;
import com.adconnect.auth.dto.AuthDtos.AuthResponse;
import com.adconnect.auth.dto.AuthDtos.ChangePasswordRequest;
import com.adconnect.auth.dto.AuthDtos.LoginRequest;
import com.adconnect.auth.dto.AuthDtos.RegisterRequest;
import com.adconnect.auth.dto.AuthDtos.UpdateProfileRequest;
import com.adconnect.auth.dto.AuthDtos.UserDto;
import com.adconnect.auth.entity.AuthProvider;
import com.adconnect.auth.entity.RefreshToken;
import com.adconnect.auth.entity.User;
import com.adconnect.auth.entity.UserStatus;
import com.adconnect.auth.mapper.UserMapper;
import com.adconnect.auth.repository.RefreshTokenRepository;
import com.adconnect.auth.repository.UserRepository;
import com.adconnect.common.events.EventPublisher;
import com.adconnect.common.events.Topics;
import com.adconnect.common.events.UserRegisteredEvent;
import com.adconnect.common.security.Role;
import com.adconnect.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper mapper;
    private final EventPublisher events;
    private final AuthProperties properties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() != Role.BRAND && request.role() != Role.INFLUENCER) {
            throw ApiException.badRequest("Only BRAND or INFLUENCER accounts can self-register");
        }
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setRole(request.role());
        user.setStatus(UserStatus.PENDING);
        users.save(user);
        publishRegistered(user);
        log.info("Registered {} user {}", user.getRole(), user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> u.getPasswordHash() != null)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ApiException.forbidden("Account suspended" + reasonSuffix(user));
        }
        user.setLastLoginAt(Instant.now());
        return issueTokens(user);
    }

    /** Rotates the refresh token: the presented token is revoked and a new pair is issued. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String presentedToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(jwtService.sha256(presentedToken))
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (stored.isRevoked()) {
            // Reuse of a rotated token suggests theft: revoke the whole family.
            refreshTokens.revokeAllForUser(stored.getUser().getId());
            throw ApiException.unauthorized("Refresh token reuse detected; please sign in again");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("Refresh token expired");
        }
        stored.setRevoked(true);
        User user = stored.getUser();
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ApiException.forbidden("Account suspended");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(String presentedToken) {
        refreshTokens.findByTokenHash(jwtService.sha256(presentedToken)).ifPresent(t -> t.setRevoked(true));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return mapper.toDto(find(userId));
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = find(userId);
        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        return mapper.toDto(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = find(userId);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(userId);
    }

    /** Finds or creates the account behind an OAuth2 login. New accounts default to INFLUENCER. */
    @Transactional
    public AuthResponse oauthLogin(String email, String fullName, AuthProvider provider) {
        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User created = new User();
            created.setEmail(email.toLowerCase());
            created.setFullName(fullName == null || fullName.isBlank() ? email : fullName);
            created.setRole(Role.INFLUENCER);
            created.setStatus(UserStatus.PENDING);
            created.setAuthProvider(provider);
            users.save(created);
            publishRegistered(created);
            return created;
        });
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ApiException.forbidden("Account suspended");
        }
        user.setLastLoginAt(Instant.now());
        return issueTokens(user);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int removed = refreshTokens.deleteExpired(Instant.now());
        log.info("Purged {} expired refresh tokens", removed);
    }

    User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private AuthResponse issueTokens(User user) {
        String refresh = jwtService.newOpaqueToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(jwtService.sha256(refresh));
        token.setExpiresAt(Instant.now().plus(properties.jwt().refreshTokenTtl()));
        refreshTokens.save(token);
        return new AuthResponse(jwtService.createAccessToken(user), refresh, jwtService.accessTtlSeconds(),
                mapper.toDto(user));
    }

    private void publishRegistered(User user) {
        events.publish(Topics.USER_REGISTERED, user.getId(), new UserRegisteredEvent(UUID.randomUUID().toString(),
                user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(), Instant.now()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String reasonSuffix(User user) {
        return user.getStatusReason() == null ? "" : ": " + user.getStatusReason();
    }
}

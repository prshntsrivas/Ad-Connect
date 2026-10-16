package com.adconnect.auth.service;

import com.adconnect.auth.dto.AuthDtos.CreateManagerRequest;
import com.adconnect.auth.dto.AuthDtos.UpdateStatusRequest;
import com.adconnect.auth.dto.AuthDtos.UserDto;
import com.adconnect.auth.dto.AuthDtos.UserStats;
import com.adconnect.auth.entity.User;
import com.adconnect.auth.entity.UserStatus;
import com.adconnect.auth.mapper.UserMapper;
import com.adconnect.auth.repository.RefreshTokenRepository;
import com.adconnect.auth.repository.UserRepository;
import com.adconnect.common.events.EventPublisher;
import com.adconnect.common.events.Topics;
import com.adconnect.common.events.UserStatusChangedEvent;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.security.Role;
import com.adconnect.common.web.ApiException;
import com.adconnect.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final UserMapper mapper;
    private final EventPublisher events;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserDto> search(Role role, UserStatus status, String q, int page, int size) {
        String query = q == null || q.isBlank() ? null : q.trim();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(users.search(role, status, query, pageable), mapper::toDto);
    }

    @Transactional
    public UserDto updateStatus(CurrentUser actor, Long userId, UpdateStatusRequest request) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getRole() == Role.ADMIN || (user.getRole() == Role.MANAGER && actor.role() != Role.ADMIN)) {
            throw ApiException.forbidden("Not allowed to change this account");
        }
        if (user.getStatus() == request.status()) {
            return mapper.toDto(user);
        }
        user.setStatus(request.status());
        user.setStatusReason(request.reason());
        if (request.status() == UserStatus.VERIFIED) {
            user.setVerifiedBy(actor.id());
            user.setVerifiedAt(Instant.now());
        }
        if (request.status() == UserStatus.SUSPENDED) {
            refreshTokens.revokeAllForUser(userId);
        }
        events.publish(Topics.USER_STATUS_CHANGED, user.getId(), new UserStatusChangedEvent(
                UUID.randomUUID().toString(), user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), request.status().name(), request.reason(), Instant.now()));
        return mapper.toDto(user);
    }

    @Transactional
    public UserDto createManager(CreateManagerRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User manager = new User();
        manager.setEmail(request.email().trim().toLowerCase());
        manager.setFullName(request.fullName().trim());
        manager.setPasswordHash(passwordEncoder.encode(request.password()));
        manager.setRole(Role.MANAGER);
        manager.setStatus(UserStatus.VERIFIED);
        manager.setVerifiedAt(Instant.now());
        return mapper.toDto(users.save(manager));
    }

    @Transactional(readOnly = true)
    public UserStats stats() {
        Map<Role, Long> byRole = new EnumMap<>(Role.class);
        Map<UserStatus, Long> byStatus = new EnumMap<>(UserStatus.class);
        long total = 0;
        for (UserRepository.RoleStatusCount row : users.countByRoleAndStatus()) {
            byRole.merge(row.getRole(), row.getTotal(), Long::sum);
            byStatus.merge(row.getStatus(), row.getTotal(), Long::sum);
            total += row.getTotal();
        }
        return new UserStats(total, byStatus.getOrDefault(UserStatus.PENDING, 0L), byRole, byStatus);
    }
}

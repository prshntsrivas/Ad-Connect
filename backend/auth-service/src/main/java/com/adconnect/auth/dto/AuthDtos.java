package com.adconnect.auth.dto;

import com.adconnect.auth.entity.UserStatus;
import com.adconnect.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain a letter and a digit")
            String password,
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "must be 10-15 digits") String phone,
            @NotNull Role role) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, long expiresIn, UserDto user) {
    }

    public record UserDto(Long id, String email, String fullName, String phone, Role role, UserStatus status,
                          String statusReason, String authProvider, Instant createdAt, Instant verifiedAt) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "must be 10-15 digits") String phone) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                    message = "must contain a letter and a digit") String newPassword) {
    }

    public record UpdateStatusRequest(@NotNull UserStatus status, @Size(max = 500) String reason) {
    }

    public record CreateManagerRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName) {
    }

    public record UserStats(long total, long pending, Map<Role, Long> byRole, Map<UserStatus, Long> byStatus) {
    }

    public record ProvidersResponse(List<String> providers) {
    }
}

package com.adconnect.auth.controller;

import com.adconnect.auth.dto.AuthDtos.AuthResponse;
import com.adconnect.auth.dto.AuthDtos.ChangePasswordRequest;
import com.adconnect.auth.dto.AuthDtos.LoginRequest;
import com.adconnect.auth.dto.AuthDtos.ProvidersResponse;
import com.adconnect.auth.dto.AuthDtos.RefreshRequest;
import com.adconnect.auth.dto.AuthDtos.RegisterRequest;
import com.adconnect.auth.dto.AuthDtos.UpdateProfileRequest;
import com.adconnect.auth.dto.AuthDtos.UserDto;
import com.adconnect.auth.service.AuthService;
import com.adconnect.common.security.AuthUser;
import com.adconnect.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    @Operation(summary = "Register a brand or influencer account (starts in PENDING until a manager verifies it)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Exchange a refresh token for a new token pair (rotating)")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    public UserDto me(@AuthUser CurrentUser user) {
        return authService.me(user.id());
    }

    @PutMapping("/me")
    public UserDto updateMe(@AuthUser CurrentUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(user.id(), request);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthUser CurrentUser user, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.id(), request);
    }

    @Operation(summary = "OAuth2 providers configured on this deployment")
    @GetMapping("/oauth2/providers")
    public ProvidersResponse providers() {
        List<String> ids = new ArrayList<>();
        ClientRegistrationRepository repository = clientRegistrations.getIfAvailable();
        if (repository instanceof InMemoryClientRegistrationRepository inMemory) {
            inMemory.forEach(r -> ids.add(r.getRegistrationId()));
        }
        return new ProvidersResponse(ids);
    }
}

package com.adconnect.auth.security;

import com.adconnect.auth.config.AuthProperties;
import com.adconnect.auth.dto.AuthDtos.AuthResponse;
import com.adconnect.auth.entity.AuthProvider;
import com.adconnect.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * After Google sign-in, issues Ad-Connect tokens and hands them to the SPA in the URL fragment
 * (fragments are never sent to servers or logged by proxies).
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final AuthProperties properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        if (email == null) {
            response.sendRedirect(properties.frontendUrl() + "/login?oauthError=email");
            return;
        }
        AuthResponse tokens = authService.oauthLogin(email, name, AuthProvider.GOOGLE);
        request.getSession().invalidate();
        response.sendRedirect(properties.frontendUrl() + "/oauth2/callback#accessToken="
                + URLEncoder.encode(tokens.accessToken(), StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(tokens.refreshToken(), StandardCharsets.UTF_8));
    }
}

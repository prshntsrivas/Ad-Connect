package com.adconnect.common.security;

import com.adconnect.common.web.ApiException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_NAME = "X-User-Name";
    public static final String USER_ROLE = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class)
                && CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        String id = request.getHeader(USER_ID);
        String role = request.getHeader(USER_ROLE);
        if (id == null || role == null) {
            AuthUser annotation = parameter.getParameterAnnotation(AuthUser.class);
            if (annotation != null && !annotation.required()) {
                return null;
            }
            throw ApiException.unauthorized("Authentication required");
        }
        try {
            String name = request.getHeader(USER_NAME);
            return new CurrentUser(Long.valueOf(id), request.getHeader(USER_EMAIL),
                    name == null ? null : URLDecoder.decode(name, StandardCharsets.UTF_8), Role.valueOf(role));
        } catch (IllegalArgumentException e) {
            throw ApiException.unauthorized("Invalid identity headers");
        }
    }
}

package com.adconnect.common.security;

import com.adconnect.common.web.ApiException;

import java.util.Arrays;

/** Identity of the caller, as authenticated by the API gateway and forwarded in X-User-* headers. */
public record CurrentUser(Long id, String email, String name, Role role) {

    public boolean is(Role... roles) {
        return Arrays.asList(roles).contains(role);
    }

    public void require(Role... roles) {
        if (!is(roles)) {
            throw ApiException.forbidden("Requires role " + Arrays.toString(roles));
        }
    }

    public boolean isStaff() {
        return is(Role.MANAGER, Role.ADMIN);
    }
}

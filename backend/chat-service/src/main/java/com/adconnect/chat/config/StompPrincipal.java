package com.adconnect.chat.config;

import com.adconnect.common.security.CurrentUser;

import java.security.Principal;

/** WebSocket session principal; its name is the user id so /user/{id}/queue/** destinations resolve. */
public record StompPrincipal(CurrentUser user) implements Principal {

    @Override
    public String getName() {
        return user.id().toString();
    }
}

package com.adconnect.chat.config;

import com.adconnect.common.security.AuthUserArgumentResolver;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.security.Role;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

/**
 * STOMP over WebSocket at /ws. The gateway authenticates the handshake (JWT in ?access_token=) and
 * forwards identity headers, which become the session principal here.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String USER_ATTR = "currentUser";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new IdentityHandshakeInterceptor())
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                                      Map<String, Object> attributes) {
                        return new StompPrincipal((CurrentUser) attributes.get(USER_ATTR));
                    }
                });
    }

    static class IdentityHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            var headers = request.getHeaders();
            String id = headers.getFirst(AuthUserArgumentResolver.USER_ID);
            String role = headers.getFirst(AuthUserArgumentResolver.USER_ROLE);
            if (id == null || role == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            String name = headers.getFirst(AuthUserArgumentResolver.USER_NAME);
            attributes.put(USER_ATTR, new CurrentUser(Long.valueOf(id),
                    headers.getFirst(AuthUserArgumentResolver.USER_EMAIL),
                    name == null ? null : URLDecoder.decode(name, StandardCharsets.UTF_8), Role.valueOf(role)));
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}

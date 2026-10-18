package com.adconnect.payment.client;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class InternalAuthConfig {

    @Bean
    public RequestInterceptor internalTokenInterceptor(@Value("${adconnect.internal-token}") String token) {
        return template -> {
            template.header("X-Internal-Token", token);
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-Id", correlationId);
            }
        };
    }
}

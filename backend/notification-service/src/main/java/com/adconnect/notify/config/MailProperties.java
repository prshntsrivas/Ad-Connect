package com.adconnect.notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adconnect.mail")
public record MailProperties(String from, String frontendUrl) {
}

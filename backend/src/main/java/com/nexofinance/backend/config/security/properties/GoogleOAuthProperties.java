package com.nexofinance.backend.config.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.oauth2.google")
public record GoogleOAuthProperties(
        String clientId
) {
}

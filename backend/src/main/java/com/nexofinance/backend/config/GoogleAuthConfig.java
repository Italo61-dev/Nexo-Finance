package com.nexofinance.backend.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nexofinance.backend.config.security.properties.GoogleOAuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class GoogleAuthConfig {

    @Bean
    @ConditionalOnMissingBean
    public GoogleIdTokenVerifier googleIdTokenVerifier(GoogleOAuthProperties googleOAuthProperties) {
        NetHttpTransport transport = new NetHttpTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(transport, jsonFactory);

        String clientId = googleOAuthProperties.clientId();
        if (clientId != null && !clientId.isBlank()) {
            builder.setAudience(Collections.singletonList(clientId));
        }
        return builder.build();
    }
}

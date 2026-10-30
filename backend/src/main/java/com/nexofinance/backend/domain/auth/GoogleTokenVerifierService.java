package com.nexofinance.backend.domain.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.nexofinance.backend.config.security.properties.GoogleOAuthProperties;
import com.nexofinance.backend.domain.auth.dto.GoogleUserInfo;
import com.nexofinance.backend.domain.auth.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final Clock clock;

    public GoogleUserInfo verify(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new InvalidTokenException("Token do Google é obrigatório.");
        }

        try {
            GoogleIdToken googleIdToken = verifier.verify(idTokenString);
            if (googleIdToken == null) {
                throw new InvalidTokenException("Token do Google inválido ou não autenticado.");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            if (payload == null) {
                throw new InvalidTokenException("Payload do token do Google inválido.");
            }

            String configuredClientId = googleOAuthProperties.clientId();
            if (configuredClientId != null && !configuredClientId.isBlank()) {
                Object aud = payload.getAudience();
                boolean audMatches = false;
                if (aud instanceof String audStr) {
                    audMatches = configuredClientId.equals(audStr);
                } else if (aud instanceof Collection<?> audList) {
                    audMatches = audList.contains(configuredClientId);
                }

                if (!audMatches) {
                    throw new InvalidTokenException("Audience do token do Google não corresponde ao Client ID configurado.");
                }
            }

            Long expirationTime = payload.getExpirationTimeSeconds();
            if (expirationTime != null && expirationTime <= clock.instant().getEpochSecond()) {
                throw new InvalidTokenException("Token do Google expirado.");
            }

            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new InvalidTokenException("Token do Google não contém e-mail.");
            }

            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified != null && !emailVerified) {
                throw new InvalidTokenException("O e-mail associado à conta Google não está verificado.");
            }

            String sub = payload.getSubject();
            String name = (String) payload.get("name");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            return new GoogleUserInfo(sub, email, name, Boolean.TRUE.equals(emailVerified));
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao validar token Google: {}", e.getMessage(), e);
            throw new InvalidTokenException("Falha na validação do token do Google: " + e.getMessage());
        }
    }
}

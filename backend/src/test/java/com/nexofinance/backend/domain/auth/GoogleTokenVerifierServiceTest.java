package com.nexofinance.backend.domain.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.nexofinance.backend.config.security.properties.GoogleOAuthProperties;
import com.nexofinance.backend.domain.auth.dto.GoogleUserInfo;
import com.nexofinance.backend.domain.auth.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierServiceTest {

    private static final String CLIENT_ID = "mock-google-client-id.apps.googleusercontent.com";
    private static final Instant FIXED_NOW = Instant.parse("2026-10-30T10:00:00Z");

    @Mock
    private GoogleIdTokenVerifier verifier;

    private GoogleOAuthProperties properties;
    private Clock fixedClock;
    private GoogleTokenVerifierService service;

    @BeforeEach
    void setUp() {
        properties = new GoogleOAuthProperties(CLIENT_ID);
        fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        service = new GoogleTokenVerifierService(verifier, properties, fixedClock);
    }

    @Test
    @DisplayName("Deve validar Google ID Token com sucesso e retornar GoogleUserInfo")
    void shouldVerifyGoogleTokenSuccessfully() throws GeneralSecurityException, IOException {
        String tokenString = "valid.google.jwt";

        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-123456");
        payload.setEmail("italo@gmail.com");
        payload.setEmailVerified(true);
        payload.set("name", "Ítalo Sousa");
        payload.setAudience(CLIENT_ID);
        payload.setExpirationTimeSeconds(FIXED_NOW.getEpochSecond() + 3600);

        when(googleIdToken.getPayload()).thenReturn(payload);
        when(verifier.verify(tokenString)).thenReturn(googleIdToken);

        GoogleUserInfo userInfo = service.verify(tokenString);

        assertThat(userInfo).isNotNull();
        assertThat(userInfo.sub()).isEqualTo("google-sub-123456");
        assertThat(userInfo.email()).isEqualTo("italo@gmail.com");
        assertThat(userInfo.name()).isEqualTo("Ítalo Sousa");
        assertThat(userInfo.emailVerified()).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar token quando a audience for uma lista contendo o client ID configurado")
    void shouldVerifyTokenWhenAudienceIsList() throws GeneralSecurityException, IOException {
        String tokenString = "valid.google.jwt";

        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-list");
        payload.setEmail("usuario@gmail.com");
        payload.setEmailVerified(true);
        payload.setAudience(List.of(CLIENT_ID, "another-aud"));
        payload.setExpirationTimeSeconds(FIXED_NOW.getEpochSecond() + 1800);

        when(googleIdToken.getPayload()).thenReturn(payload);
        when(verifier.verify(tokenString)).thenReturn(googleIdToken);

        GoogleUserInfo userInfo = service.verify(tokenString);

        assertThat(userInfo).isNotNull();
        assertThat(userInfo.sub()).isEqualTo("google-sub-list");
        assertThat(userInfo.email()).isEqualTo("usuario@gmail.com");
        assertThat(userInfo.name()).isEqualTo("usuario");
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o token for nulo ou vazio")
    void shouldThrowWhenTokenIsNullOrEmpty() {
        assertThatThrownBy(() -> service.verify(null))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token do Google é obrigatório.");

        assertThatThrownBy(() -> service.verify("   "))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token do Google é obrigatório.");
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o validador retornar nulo (token inválido/não assinado)")
    void shouldThrowWhenTokenIsInvalidAccordingToVerifier() throws GeneralSecurityException, IOException {
        when(verifier.verify(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.verify("invalid.token.here"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token do Google inválido ou não autenticado.");
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando a verificação lançar exceção")
    void shouldThrowWhenVerifierThrowsException() throws GeneralSecurityException, IOException {
        when(verifier.verify(anyString())).thenThrow(new GeneralSecurityException("Assinatura corrompida"));

        assertThatThrownBy(() -> service.verify("corrupted.token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageStartingWith("Falha na validação do token do Google");
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o token do Google estiver expirado")
    void shouldThrowWhenTokenIsExpired() throws GeneralSecurityException, IOException {
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-expired");
        payload.setEmail("expired@gmail.com");
        payload.setEmailVerified(true);
        payload.setAudience(CLIENT_ID);
        payload.setExpirationTimeSeconds(FIXED_NOW.getEpochSecond() - 10); // Expirado há 10 segundos

        when(googleIdToken.getPayload()).thenReturn(payload);
        when(verifier.verify("expired.token")).thenReturn(googleIdToken);

        assertThatThrownBy(() -> service.verify("expired.token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token do Google expirado.");
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o client-id (audience) for divergente")
    void shouldThrowWhenAudienceDoesNotMatch() throws GeneralSecurityException, IOException {
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-wrong-aud");
        payload.setEmail("hacker@gmail.com");
        payload.setEmailVerified(true);
        payload.setAudience("different-client-id.apps.googleusercontent.com");
        payload.setExpirationTimeSeconds(FIXED_NOW.getEpochSecond() + 3600);

        when(googleIdToken.getPayload()).thenReturn(payload);
        when(verifier.verify("wrong.audience.token")).thenReturn(googleIdToken);

        assertThatThrownBy(() -> service.verify("wrong.audience.token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Audience do token do Google não corresponde ao Client ID configurado.");
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o token não contiver e-mail")
    void shouldThrowWhenEmailIsMissing() throws GeneralSecurityException, IOException {
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-no-email");
        payload.setAudience(CLIENT_ID);
        payload.setExpirationTimeSeconds(FIXED_NOW.getEpochSecond() + 3600);

        when(googleIdToken.getPayload()).thenReturn(payload);
        when(verifier.verify("no.email.token")).thenReturn(googleIdToken);

        assertThatThrownBy(() -> service.verify("no.email.token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token do Google não contém e-mail.");
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o e-mail não for verificado pela Google")
    void shouldThrowWhenEmailNotVerified() throws GeneralSecurityException, IOException {
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-unverified");
        payload.setEmail("unverified@gmail.com");
        payload.setEmailVerified(false);
        payload.setAudience(CLIENT_ID);
        payload.setExpirationTimeSeconds(FIXED_NOW.getEpochSecond() + 3600);

        when(googleIdToken.getPayload()).thenReturn(payload);
        when(verifier.verify("unverified.token")).thenReturn(googleIdToken);

        assertThatThrownBy(() -> service.verify("unverified.token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("O e-mail associado à conta Google não está verificado.");
    }
}

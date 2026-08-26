package com.nexofinance.backend.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.nexofinance.backend.config.security.properties.JwtProperties;
import com.nexofinance.backend.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_SECONDS = 3600; // 1 hora
    private static final String ISSUER = "nexo-finance";

    private JwtProperties jwtProperties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(SECRET_KEY, EXPIRATION_SECONDS, ISSUER);
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    @DisplayName("Deve gerar token JWT com subject, claims e emissor corretos")
    void shouldGenerateJwtTokenWithCorrectClaims() {
        User user = User.builder()
                .id(10L)
                .name("Ítalo Sousa")
                .email("italo@nexofinance.com")
                .build();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).contains("italo@nexofinance.com");
        assertThat(jwtService.extractUserId(token)).contains(10L);
    }

    @Test
    @DisplayName("Deve retornar Optional vazio ao tentar extrair claims de token com assinatura inválida")
    void shouldReturnEmptyOptionalForInvalidSignatureToken() {
        String forgedToken = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("hacker@fake.com")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256("outra_chave_secreta_completamente_diferente"));

        assertThat(jwtService.isTokenValid(forgedToken)).isFalse();
        assertThat(jwtService.extractSubject(forgedToken)).isEmpty();
        assertThat(jwtService.extractUserId(forgedToken)).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar Optional vazio e considerar token inválido quando estiver expirado")
    void shouldReturnEmptyOptionalWhenTokenIsExpired() {
        String expiredToken = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("user@test.com")
                .withIssuedAt(Instant.now().minusSeconds(100))
                .withExpiresAt(Instant.now().minusSeconds(10))
                .sign(Algorithm.HMAC256(SECRET_KEY));

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
        assertThat(jwtService.extractSubject(expiredToken)).isEmpty();
        assertThat(jwtService.extractUserId(expiredToken)).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar Optional vazio para tokens nulos ou em branco")
    void shouldReturnEmptyOptionalForNullOrBlankToken() {
        assertThat(jwtService.isTokenValid(null)).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
        assertThat(jwtService.extractSubject(null)).isEmpty();
        assertThat(jwtService.extractUserId("")).isEmpty();
    }
}

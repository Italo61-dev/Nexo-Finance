package com.nexofinance.backend.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nexofinance.backend.config.security.properties.JwtProperties;
import com.nexofinance.backend.domain.auth.TokenService;
import com.nexofinance.backend.domain.user.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class JwtService implements TokenService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_NAME = "name";

    private final JwtProperties jwtProperties;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.algorithm = Algorithm.HMAC256(jwtProperties.secret());
        this.verifier = JWT.require(this.algorithm)
                .withIssuer(jwtProperties.issuer())
                .build();
    }

    @Override
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.expirationSeconds());

        return JWT.create()
                .withIssuer(jwtProperties.issuer())
                .withSubject(user.getEmail())
                .withClaim(CLAIM_USER_ID, user.getId())
                .withClaim(CLAIM_NAME, user.getName())
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    @Override
    public Optional<String> extractSubject(String token) {
        return decodeTokenSafely(token).map(DecodedJWT::getSubject);
    }

    @Override
    public Optional<Long> extractUserId(String token) {
        return decodeTokenSafely(token)
                .map(jwt -> jwt.getClaim(CLAIM_USER_ID).asLong());
    }

    @Override
    public boolean isTokenValid(String token) {
        return decodeTokenSafely(token).isPresent();
    }

    @Override
    public long getExpirationSeconds() {
        return jwtProperties.expirationSeconds();
    }

    private Optional<DecodedJWT> decodeTokenSafely(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException exception) {
            return Optional.empty();
        }
    }
}

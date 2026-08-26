package com.nexofinance.backend.domain.auth;

import com.nexofinance.backend.domain.user.User;

import java.util.Optional;

public interface TokenService {

    String generateToken(User user);

    Optional<String> extractSubject(String token);

    Optional<Long> extractUserId(String token);

    boolean isTokenValid(String token);

    long getExpirationSeconds();
}

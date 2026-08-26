package com.nexofinance.backend.domain.auth.dto;

public record AuthResponseDTO(
        String token,
        String tokenType,
        Long expiresInSeconds
) {
    public static AuthResponseDTO of(String token, Long expiresInSeconds) {
        return new AuthResponseDTO(token, "Bearer", expiresInSeconds);
    }
}

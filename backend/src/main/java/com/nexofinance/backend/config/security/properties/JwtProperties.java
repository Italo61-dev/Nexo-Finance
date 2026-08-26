package com.nexofinance.backend.config.security.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank(message = "A chave secreta JWT é obrigatória")
        String secret,

        @Positive(message = "O tempo de expiração do JWT deve ser maior que zero")
        long expirationSeconds,

        @NotBlank(message = "O emissor (issuer) do JWT é obrigatório")
        String issuer
) {
}

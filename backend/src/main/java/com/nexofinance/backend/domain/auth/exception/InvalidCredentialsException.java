package com.nexofinance.backend.domain.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciais inválidas: e-mail ou senha incorretos");
    }
}

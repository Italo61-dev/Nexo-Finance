package com.nexofinance.backend.domain.user.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super(String.format("Já existe um usuário cadastrado com o e-mail '%s'", email));
    }
}

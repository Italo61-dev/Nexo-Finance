package com.nexofinance.backend.domain.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super(String.format("Usuário não encontrado para o ID: %d", id));
    }
}

package com.nexofinance.backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        List<FieldValidationErrorDTO> validationErrors
) {
    public record FieldValidationErrorDTO(
            String field,
            String message
    ) {
    }

    public static ErrorResponseDTO of(int status, String error, String message, String path) {
        return new ErrorResponseDTO(status, error, message, path, LocalDateTime.now(), null);
    }

    public static ErrorResponseDTO withValidationErrors(int status, String error, String message, String path, List<FieldValidationErrorDTO> validationErrors) {
        return new ErrorResponseDTO(status, error, message, path, LocalDateTime.now(), validationErrors);
    }
}

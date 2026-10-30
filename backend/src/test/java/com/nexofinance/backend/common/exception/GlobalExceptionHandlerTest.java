package com.nexofinance.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    @DisplayName("Deve tratar HttpMessageNotReadableException retornando 400 Bad Request")
    void shouldHandleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", (org.springframework.http.HttpInputMessage) null);

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleHttpMessageNotReadableException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("Corpo da requisição inválido ou malformado");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentTypeMismatchException retornando 400 Bad Request")
    void shouldHandleMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, null
        );

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleMethodArgumentTypeMismatchException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("id");
    }

    @Test
    @DisplayName("Deve tratar HttpRequestMethodNotSupportedException retornando 405 Method Not Allowed")
    void shouldHandleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleHttpRequestMethodNotSupportedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(405);
        assertThat(response.getBody().message()).contains("POST");
    }

    @Test
    @DisplayName("Deve tratar DataIntegrityViolationException retornando 409 Conflict")
    void shouldHandleDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique constraint violation");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleDataIntegrityViolationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("Violação de integridade nos dados");
    }

    @Test
    @DisplayName("Deve tratar NoResourceFoundException retornando 404 Not Found")
    void shouldHandleNoResourceFoundException() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown", "Resource not found");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleNoResourceFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Recurso não encontrado");
    }

    @Test
    @DisplayName("Deve tratar Exception genérica retornando 500 Internal Server Error")
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Ocorreu um erro interno no servidor");
    }
}

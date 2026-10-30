package com.nexofinance.backend.common.exception;

import com.nexofinance.backend.domain.auth.exception.InvalidCredentialsException;
import com.nexofinance.backend.domain.auth.exception.InvalidPasswordException;
import com.nexofinance.backend.domain.auth.exception.InvalidTokenException;
import com.nexofinance.backend.domain.user.exception.EmailAlreadyExistsException;
import com.nexofinance.backend.domain.user.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCredentialsException(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDTO);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidTokenException(
            InvalidTokenException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidPasswordException(
            InvalidPasswordException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserNotFoundException(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDTO);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponseDTO);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponseDTO.FieldValidationErrorDTO> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponseDTO.FieldValidationErrorDTO(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.withValidationErrors(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Erro de validação nos campos informados",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        log.warn("Corpo da requisição inválido ou malformado [{}]: {}", request.getRequestURI(), exception.getMessage());
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Corpo da requisição inválido ou malformado",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String paramName = exception.getName();
        String message = String.format("O parâmetro '%s' recebeu um valor com formato inválido", paramName);
        log.warn("Parâmetro inválido na requisição [{}]: {}", request.getRequestURI(), exception.getMessage());
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String message = String.format("Método HTTP '%s' não é suportado para este endpoint", exception.getMethod());
        log.warn("Método HTTP não suportado [{}]: {}", request.getRequestURI(), exception.getMessage());
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponseDTO);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("Violação de integridade no banco de dados [{}]: {}", request.getRequestURI(), exception.getMessage());
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                "Violação de integridade nos dados: recurso já existente ou em conflito",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponseDTO);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "Recurso não encontrado",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDTO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Erro inesperado ao processar requisição [{}]: ", request.getRequestURI(), exception);
        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ocorreu um erro interno no servidor",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDTO);
    }
}

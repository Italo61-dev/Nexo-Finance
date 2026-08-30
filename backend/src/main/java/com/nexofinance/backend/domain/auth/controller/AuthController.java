package com.nexofinance.backend.domain.auth.controller;

import com.nexofinance.backend.domain.auth.AuthService;
import com.nexofinance.backend.domain.auth.dto.AuthResponseDTO;
import com.nexofinance.backend.domain.auth.dto.ForgotPasswordRequestDTO;
import com.nexofinance.backend.domain.auth.dto.LoginRequestDTO;
import com.nexofinance.backend.domain.auth.dto.MessageResponseDTO;
import com.nexofinance.backend.domain.auth.dto.ResetPasswordRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> authenticateUser(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO
    ) {
        AuthResponseDTO responseDTO = authService.authenticate(loginRequestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO requestDTO
    ) {
        MessageResponseDTO response = authService.requestPasswordReset(requestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO requestDTO
    ) {
        MessageResponseDTO response = authService.resetPassword(requestDTO);
        return ResponseEntity.ok(response);
    }
}

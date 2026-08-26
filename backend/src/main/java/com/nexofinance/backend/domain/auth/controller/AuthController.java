package com.nexofinance.backend.domain.auth.controller;

import com.nexofinance.backend.domain.auth.AuthService;
import com.nexofinance.backend.domain.auth.dto.AuthResponseDTO;
import com.nexofinance.backend.domain.auth.dto.LoginRequestDTO;
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
}

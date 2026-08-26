package com.nexofinance.backend.domain.auth;

import com.nexofinance.backend.domain.auth.dto.AuthResponseDTO;
import com.nexofinance.backend.domain.auth.dto.LoginRequestDTO;
import com.nexofinance.backend.domain.auth.exception.InvalidCredentialsException;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve autenticar usuário com credenciais válidas e retornar token JWT")
    void shouldAuthenticateUserSuccessfullyWithValidCredentials() {
        LoginRequestDTO loginDTO = new LoginRequestDTO("italo@nexofinance.com", "password123");

        User user = User.builder()
                .id(1L)
                .name("Ítalo Sousa")
                .email("italo@nexofinance.com")
                .passwordHash("encoded_hash")
                .build();

        when(userRepository.findByEmail("italo@nexofinance.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("jwt_sample_token");
        when(tokenService.getExpirationSeconds()).thenReturn(86400L);

        AuthResponseDTO response = authService.authenticate(loginDTO);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt_sample_token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(86400L);

        verify(userRepository).findByEmail("italo@nexofinance.com");
        verify(passwordEncoder).matches("password123", "encoded_hash");
        verify(tokenService).generateToken(user);
    }

    @Test
    @DisplayName("Deve lançar InvalidCredentialsException quando o e-mail não for encontrado")
    void shouldThrowInvalidCredentialsExceptionWhenEmailNotFound() {
        LoginRequestDTO loginDTO = new LoginRequestDTO("naoexiste@nexofinance.com", "password123");

        when(userRepository.findByEmail("naoexiste@nexofinance.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(loginDTO))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais inválidas: e-mail ou senha incorretos");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve lançar InvalidCredentialsException quando a senha informada for incorreta")
    void shouldThrowInvalidCredentialsExceptionWhenPasswordIsIncorrect() {
        LoginRequestDTO loginDTO = new LoginRequestDTO("italo@nexofinance.com", "senha_errada");

        User user = User.builder()
                .id(1L)
                .name("Ítalo")
                .email("italo@nexofinance.com")
                .passwordHash("encoded_hash")
                .build();

        when(userRepository.findByEmail("italo@nexofinance.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha_errada", "encoded_hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(loginDTO))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais inválidas: e-mail ou senha incorretos");

        verify(tokenService, never()).generateToken(any());
    }
}

package com.nexofinance.backend.domain.auth;

import com.nexofinance.backend.domain.auth.dto.AuthResponseDTO;
import com.nexofinance.backend.domain.auth.dto.ForgotPasswordRequestDTO;
import com.nexofinance.backend.domain.auth.dto.LoginRequestDTO;
import com.nexofinance.backend.domain.auth.dto.MessageResponseDTO;
import com.nexofinance.backend.domain.auth.dto.ResetPasswordRequestDTO;
import com.nexofinance.backend.domain.auth.exception.InvalidCredentialsException;
import com.nexofinance.backend.domain.auth.exception.InvalidPasswordException;
import com.nexofinance.backend.domain.auth.exception.InvalidTokenException;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

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
                .active(true)
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
                .active(true)
                .build();

        when(userRepository.findByEmail("italo@nexofinance.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha_errada", "encoded_hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(loginDTO))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais inválidas: e-mail ou senha incorretos");

        verify(tokenService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve solicitar recuperação de senha para usuário ativo e disparar envio de e-mail")
    void shouldRequestPasswordResetSuccessfullyForExistingActiveUser() {
        ForgotPasswordRequestDTO requestDTO = new ForgotPasswordRequestDTO("italo@nexofinance.com");

        User user = User.builder()
                .id(1L)
                .name("Ítalo")
                .email("italo@nexofinance.com")
                .active(true)
                .build();

        when(userRepository.findByEmail("italo@nexofinance.com")).thenReturn(Optional.of(user));

        MessageResponseDTO response = authService.requestPasswordReset(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("Se o e-mail informado estiver cadastrado");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("italo@nexofinance.com"), anyString());
    }

    @Test
    @DisplayName("Não deve disparar e-mail se o e-mail não existir na base, mantendo resposta neutra")
    void shouldNotSendEmailWhenUserDoesNotExistDuringPasswordResetRequest() {
        ForgotPasswordRequestDTO requestDTO = new ForgotPasswordRequestDTO("naoexiste@nexofinance.com");

        when(userRepository.findByEmail("naoexiste@nexofinance.com")).thenReturn(Optional.empty());

        MessageResponseDTO response = authService.requestPasswordReset(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("Se o e-mail informado estiver cadastrado");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Não deve disparar e-mail se o usuário estiver inativo")
    void shouldNotSendEmailWhenUserIsInactiveDuringPasswordResetRequest() {
        ForgotPasswordRequestDTO requestDTO = new ForgotPasswordRequestDTO("inativo@nexofinance.com");

        User inactiveUser = User.builder()
                .id(2L)
                .name("Usuário Inativo")
                .email("inativo@nexofinance.com")
                .active(false)
                .build();

        when(userRepository.findByEmail("inativo@nexofinance.com")).thenReturn(Optional.of(inactiveUser));

        MessageResponseDTO response = authService.requestPasswordReset(requestDTO);

        assertThat(response).isNotNull();
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve redefinir senha com sucesso quando o token for válido e ativo")
    void shouldResetPasswordSuccessfullyWhenTokenIsValid() {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("valid-token-uuid", "NewPassword123!");

        User user = User.builder()
                .id(1L)
                .name("Ítalo")
                .email("italo@nexofinance.com")
                .passwordHash("old_hash")
                .active(true)
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(10L)
                .token("valid-token-uuid")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(20))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken("valid-token-uuid")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("NewPassword123!", "old_hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new_encoded_hash");

        MessageResponseDTO response = authService.resetPassword(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("Senha redefinida com sucesso");
        assertThat(resetToken.getUsed()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("new_encoded_hash");

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    @DisplayName("Deve lançar InvalidPasswordException quando a nova senha for igual à senha atual")
    void shouldThrowInvalidPasswordExceptionWhenNewPasswordMatchesCurrentPassword() {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("valid-token-uuid", "CurrentPassword123!");

        User user = User.builder()
                .id(1L)
                .passwordHash("current_encoded_hash")
                .active(true)
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("valid-token-uuid")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(20))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken("valid-token-uuid")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("CurrentPassword123!", "current_encoded_hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.resetPassword(requestDTO))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("A nova senha não pode ser igual à senha anterior.");

        verify(userRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o token não for encontrado")
    void shouldThrowInvalidTokenExceptionWhenTokenNotFound() {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("token-inexistente", "newPassword123");

        when(passwordResetTokenRepository.findByToken("token-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(requestDTO))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token de recuperação inválido ou inexistente.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o token já estiver expirado")
    void shouldThrowInvalidTokenExceptionWhenTokenIsExpired() {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("token-expirado", "newPassword123");

        User user = User.builder().id(1L).active(true).build();
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .token("token-expirado")
                .user(user)
                .expiryDate(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.resetPassword(requestDTO))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("O token de recuperação expirou. Solicite um novo link.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando o token já tiver sido utilizado")
    void shouldThrowInvalidTokenExceptionWhenTokenAlreadyUsed() {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("token-utilizado", "newPassword123");

        User user = User.builder().id(1L).active(true).build();
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .token("token-utilizado")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(20))
                .used(true)
                .build();

        when(passwordResetTokenRepository.findByToken("token-utilizado")).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> authService.resetPassword(requestDTO))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Este token de recuperação já foi utilizado.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando a conta do usuário estiver inativa")
    void shouldThrowInvalidTokenExceptionWhenUserIsInactiveDuringReset() {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("valid-token", "newPassword123");

        User inactiveUser = User.builder().id(1L).active(false).build();
        PasswordResetToken validToken = PasswordResetToken.builder()
                .token("valid-token")
                .user(inactiveUser)
                .expiryDate(LocalDateTime.now().plusMinutes(20))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken));

        assertThatThrownBy(() -> authService.resetPassword(requestDTO))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("A conta do usuário encontra-se inativa.");

        verify(userRepository, never()).save(any());
    }
}

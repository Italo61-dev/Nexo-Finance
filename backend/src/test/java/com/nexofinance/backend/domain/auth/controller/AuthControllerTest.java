package com.nexofinance.backend.domain.auth.controller;

import com.nexofinance.backend.domain.auth.AuthService;
import com.nexofinance.backend.domain.auth.dto.AuthResponseDTO;
import com.nexofinance.backend.domain.auth.dto.ForgotPasswordRequestDTO;
import com.nexofinance.backend.domain.auth.dto.LoginRequestDTO;
import com.nexofinance.backend.domain.auth.dto.MessageResponseDTO;
import com.nexofinance.backend.domain.auth.dto.ResetPasswordRequestDTO;
import com.nexofinance.backend.domain.auth.exception.InvalidCredentialsException;
import com.nexofinance.backend.domain.auth.exception.InvalidPasswordException;
import com.nexofinance.backend.domain.auth.exception.InvalidTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Deve autenticar usuário com credenciais válidas e retornar status 200 OK com token JWT")
    void shouldAuthenticateUserSuccessfullyWithValidCredentials() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("italo@nexofinance.com", "password123");
        AuthResponseDTO authResponse = AuthResponseDTO.of("mocked_jwt_token", 86400L);

        when(authService.authenticate(any(LoginRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked_jwt_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(86400L));
    }

    @Test
    @DisplayName("Deve retornar status 400 Bad Request quando a validação dos campos de login falhar")
    void shouldReturnBadRequestWhenLoginValidationFails() throws Exception {
        LoginRequestDTO invalidLogin = new LoginRequestDTO("email_invalido", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("Deve retornar status 401 Unauthorized quando as credenciais forem incorretas ou inexistentes")
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        LoginRequestDTO wrongLogin = new LoginRequestDTO("italo@nexofinance.com", "senha_errada");

        when(authService.authenticate(any(LoginRequestDTO.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas: e-mail ou senha incorretos"));
    }

    @Test
    @DisplayName("Deve retornar status 200 OK ao solicitar recuperação de senha com e-mail válido")
    void shouldReturnOkWhenRequestingPasswordResetWithValidEmail() throws Exception {
        ForgotPasswordRequestDTO requestDTO = new ForgotPasswordRequestDTO("italo@nexofinance.com");
        MessageResponseDTO responseDTO = new MessageResponseDTO("Se o e-mail informado estiver cadastrado, as instruções foram enviadas.");

        when(authService.requestPasswordReset(any(ForgotPasswordRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Se o e-mail informado estiver cadastrado, as instruções foram enviadas."));
    }

    @Test
    @DisplayName("Deve retornar status 400 Bad Request ao solicitar recuperação de senha com e-mail inválido")
    void shouldReturnBadRequestWhenRequestingPasswordResetWithInvalidEmail() throws Exception {
        ForgotPasswordRequestDTO requestDTO = new ForgotPasswordRequestDTO("formato_invalido");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("Deve retornar status 200 OK ao redefinir senha com token e nova senha válidos")
    void shouldReturnOkWhenResettingPasswordWithValidData() throws Exception {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("valid-token-123", "NewPassword123!");
        MessageResponseDTO responseDTO = new MessageResponseDTO("Senha redefinida com sucesso.");

        when(authService.resetPassword(any(ResetPasswordRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha redefinida com sucesso."));
    }

    @Test
    @DisplayName("Deve retornar status 400 Bad Request ao redefinir senha com senha que viola a política de segurança")
    void shouldReturnBadRequestWhenResettingPasswordWithWeakPassword() throws Exception {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("valid-token-123", "fraca");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("Deve retornar status 400 Bad Request quando a nova senha for igual à anterior")
    void shouldReturnBadRequestWhenNewPasswordMatchesPreviousPassword() throws Exception {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("valid-token-123", "SamePassword123!");

        when(authService.resetPassword(any(ResetPasswordRequestDTO.class)))
                .thenThrow(new InvalidPasswordException("A nova senha não pode ser igual à senha anterior."));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("A nova senha não pode ser igual à senha anterior."));
    }

    @Test
    @DisplayName("Deve retornar status 400 Bad Request e ErrorResponseDTO quando o token for inválido ou expirado")
    void shouldReturnBadRequestWhenResetPasswordTokenIsInvalid() throws Exception {
        ResetPasswordRequestDTO requestDTO = new ResetPasswordRequestDTO("token-expirado", "NewPassword123!");

        when(authService.resetPassword(any(ResetPasswordRequestDTO.class)))
                .thenThrow(new InvalidTokenException("O token de recuperação expirou. Solicite um novo link."));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("O token de recuperação expirou. Solicite um novo link."));
    }
}

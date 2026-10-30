package com.nexofinance.backend.domain.auth;

import com.nexofinance.backend.domain.auth.dto.GoogleLoginRequestDTO;
import com.nexofinance.backend.domain.auth.dto.GoogleUserInfo;
import com.nexofinance.backend.domain.auth.dto.LoginRequestDTO;
import com.nexofinance.backend.domain.user.AuthProvider;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import com.nexofinance.backend.domain.user.dto.RegisterUserRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Test
    @DisplayName("Deve cadastrar novo usuário de ponta a ponta e persistir com senha hasheada")
    void shouldRegisterNewUserAndPersistSuccessfully() throws Exception {
        RegisterUserRequestDTO registerDTO = new RegisterUserRequestDTO(
                "Usuário Teste Integração",
                "integration_register@nexofinance.com",
                "SenhaForte123@"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Usuário Teste Integração"))
                .andExpect(jsonPath("$.email").value("integration_register@nexofinance.com"));

        Optional<User> persistedUser = userRepository.findByEmail("integration_register@nexofinance.com");
        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().getPasswordHash()).isNotEqualTo("SenhaForte123@");
        assertThat(persistedUser.get().getPasswordHash()).startsWith("$2a$");
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar access token JWT válido")
    void shouldAuthenticateSuccessfully() throws Exception {
        RegisterUserRequestDTO registerDTO = new RegisterUserRequestDTO(
                "Usuário Login",
                "integration_login@nexofinance.com",
                "SenhaForte123@"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());

        LoginRequestDTO loginDTO = new LoginRequestDTO(
                "integration_login@nexofinance.com",
                "SenhaForte123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(86400));
    }

    @Test
    @DisplayName("Deve falhar no login com status 401 Unauthorized ao informar senha incorreta")
    void shouldFailLoginWithInvalidPassword() throws Exception {
        RegisterUserRequestDTO registerDTO = new RegisterUserRequestDTO(
                "Usuário Senha Errada",
                "integration_wrongpass@nexofinance.com",
                "SenhaForte123@"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());

        LoginRequestDTO loginDTO = new LoginRequestDTO(
                "integration_wrongpass@nexofinance.com",
                "SenhaIncorreta999@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Deve falhar no login com status 401 Unauthorized para e-mail inexistente")
    void shouldFailLoginWithNonExistentEmail() throws Exception {
        LoginRequestDTO loginDTO = new LoginRequestDTO(
                "naoexiste@nexofinance.com",
                "QualquerSenha123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Deve falhar no cadastro com status 409 Conflict ao tentar cadastrar e-mail já existente")
    void shouldFailRegistrationWhenEmailAlreadyExists() throws Exception {
        RegisterUserRequestDTO registerDTO = new RegisterUserRequestDTO(
                "Primeiro Cadastro",
                "integration_duplicado@nexofinance.com",
                "SenhaForte123@"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());

        RegisterUserRequestDTO duplicateDTO = new RegisterUserRequestDTO(
                "Segundo Cadastro",
                "integration_duplicado@nexofinance.com",
                "OutraSenha456@"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Deve autenticar e criar automaticamente novo usuário via Google (JIT)")
    void shouldAuthenticateWithGoogleAndProvisionNewUserSuccessfully() throws Exception {
        String googleIdToken = "dummy-google-token-jit";
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(
                "sub-google-integration-123",
                "integration_google_jit@nexofinance.com",
                "Google User Integration",
                true
        );

        when(googleTokenVerifierService.verify(googleIdToken)).thenReturn(googleUserInfo);

        GoogleLoginRequestDTO loginRequest = new GoogleLoginRequestDTO(googleIdToken);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(86400));

        Optional<User> createdUser = userRepository.findByEmail("integration_google_jit@nexofinance.com");
        assertThat(createdUser).isPresent();
        assertThat(createdUser.get().getName()).isEqualTo("Google User Integration");
        assertThat(createdUser.get().getPasswordHash()).isNull();
        assertThat(createdUser.get().getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(createdUser.get().getProviderId()).isEqualTo("sub-google-integration-123");
        assertThat(createdUser.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("Deve autenticar usuário existente via Google sem duplicar registro no banco")
    void shouldAuthenticateExistingUserWithGoogleWithoutDuplicating() throws Exception {
        RegisterUserRequestDTO registerDTO = new RegisterUserRequestDTO(
                "Usuário Existente",
                "integration_google_existing@nexofinance.com",
                "SenhaForte123@"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());

        String googleIdToken = "dummy-google-token-existing";
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(
                "sub-google-existing-456",
                "integration_google_existing@nexofinance.com",
                "Usuário Existente",
                true
        );

        when(googleTokenVerifierService.verify(googleIdToken)).thenReturn(googleUserInfo);

        GoogleLoginRequestDTO loginRequest = new GoogleLoginRequestDTO(googleIdToken);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(86400));

        Optional<User> user = userRepository.findByEmail("integration_google_existing@nexofinance.com");
        assertThat(user).isPresent();
        assertThat(user.get().getProviderId()).isEqualTo("sub-google-existing-456");
        assertThat(user.get().getPasswordHash()).isNotNull();
    }
}

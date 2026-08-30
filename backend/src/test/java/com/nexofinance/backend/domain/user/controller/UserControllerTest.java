package com.nexofinance.backend.domain.user.controller;

import com.nexofinance.backend.domain.auth.TokenService;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import com.nexofinance.backend.domain.user.UserService;
import com.nexofinance.backend.domain.user.dto.RegisterUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UpdateUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UserResponseDTO;
import com.nexofinance.backend.domain.user.exception.EmailAlreadyExistsException;
import com.nexofinance.backend.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    // --- CREATE TESTS ---

    @Test
    @DisplayName("Deve cadastrar novo usuário com sucesso e retornar status 201 Created com header Location")
    void shouldCreateNewUserAccountSuccessfully() throws Exception {
        RegisterUserRequestDTO requestDTO = new RegisterUserRequestDTO(
                "Ítalo Sousa",
                "italo@nexofinance.com",
                "Password123!"
        );

        UserResponseDTO responseDTO = new UserResponseDTO(
                1L,
                "Ítalo Sousa",
                "italo@nexofinance.com",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.register(any(RegisterUserRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/users")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/users/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Ítalo Sousa"))
                .andExpect(jsonPath("$.email").value("italo@nexofinance.com"));
    }

    @Test
    @DisplayName("Deve retornar status 400 Bad Request quando a validação dos campos obrigatórios falhar no cadastro")
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        RegisterUserRequestDTO invalidRequestDTO = new RegisterUserRequestDTO(
                "",
                "email_invalido",
                "123"
        );

        mockMvc.perform(post("/api/v1/users")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("Deve retornar status 409 Conflict quando o e-mail informado já estiver cadastrado")
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        RegisterUserRequestDTO requestDTO = new RegisterUserRequestDTO(
                "Usuario Existente",
                "duplicado@nexofinance.com",
                "Password123!"
        );

        when(userService.register(any(RegisterUserRequestDTO.class)))
                .thenThrow(new EmailAlreadyExistsException("duplicado@nexofinance.com"));

        mockMvc.perform(post("/api/v1/users")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Já existe um usuário cadastrado com o e-mail 'duplicado@nexofinance.com'"));
    }

    // --- READ BY ID TESTS ---

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso utilizando token Bearer JWT real")
    void shouldFindUserByIdWithRealBearerToken() throws Exception {
        String uniqueEmail = "real.bearer." + System.currentTimeMillis() + "@nexofinance.com";
        User user = userRepository.save(User.builder()
                .name("Ítalo Real")
                .email(uniqueEmail)
                .passwordHash("encoded_pass")
                .build());

        String token = tokenService.generateToken(user);

        UserResponseDTO responseDTO = new UserResponseDTO(
                user.getId(),
                "Ítalo Real",
                uniqueEmail,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.findUserById(user.getId())).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/users/" + user.getId())
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Ítalo Real"))
                .andExpect(jsonPath("$.email").value(uniqueEmail));
    }

    @Test
    @DisplayName("Deve retornar status 401 Unauthorized ao tentar buscar usuário por ID sem autenticação")
    void shouldReturnUnauthorizedWhenFindingUserByIdWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/1")
                        .contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve buscar usuário por ID e retornar status 200 OK")
    void shouldFindUserByIdSuccessfully() throws Exception {
        UserResponseDTO responseDTO = new UserResponseDTO(
                1L,
                "Ítalo Sousa",
                "italo@nexofinance.com",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.findUserById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/users/1")
                        .contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Ítalo Sousa"))
                .andExpect(jsonPath("$.email").value("italo@nexofinance.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar status 404 Not Found quando usuário não for encontrado por ID")
    void shouldReturnNotFoundWhenUserDoesNotExistById() throws Exception {
        when(userService.findUserById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/v1/users/99")
                        .contextPath("/api/v1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado para o ID: 99"));
    }

    // --- READ ALL (PAGINATED) TESTS ---

    @Test
    @DisplayName("Deve retornar status 401 Unauthorized ao tentar listar usuários sem autenticação")
    void shouldReturnUnauthorizedWhenListingUsersWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve listar usuários de forma paginada e retornar status 200 OK")
    void shouldFindAllUsersPaginated() throws Exception {
        UserResponseDTO userDTO = new UserResponseDTO(
                1L,
                "Ítalo Sousa",
                "italo@nexofinance.com",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.findAllUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(userDTO)));

        mockMvc.perform(get("/api/v1/users")
                        .contextPath("/api/v1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Ítalo Sousa"));
    }

    // --- UPDATE TESTS ---

    @Test
    @DisplayName("Deve retornar status 401 Unauthorized ao tentar atualizar usuário sem autenticação")
    void shouldReturnUnauthorizedWhenUpdatingUserWithoutAuth() throws Exception {
        UpdateUserRequestDTO updateDTO = new UpdateUserRequestDTO("Novo Nome", "novo@test.com");

        mockMvc.perform(put("/api/v1/users/1")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve atualizar perfil do usuário com sucesso e retornar status 200 OK")
    void shouldUpdateUserProfileSuccessfully() throws Exception {
        UpdateUserRequestDTO updateDTO = new UpdateUserRequestDTO("Novo Nome", "novo.email@nexofinance.com");

        UserResponseDTO updatedUserDTO = new UserResponseDTO(
                1L,
                "Novo Nome",
                "novo.email@nexofinance.com",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.updateUserProfile(eq(1L), any(UpdateUserRequestDTO.class))).thenReturn(updatedUserDTO);

        mockMvc.perform(put("/api/v1/users/1")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Novo Nome"))
                .andExpect(jsonPath("$.email").value("novo.email@nexofinance.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar status 404 Not Found ao tentar atualizar usuário inexistente")
    void shouldReturnNotFoundWhenUpdatingNonExistentUser() throws Exception {
        UpdateUserRequestDTO updateDTO = new UpdateUserRequestDTO("Nome", "email@test.com");

        when(userService.updateUserProfile(eq(99L), any(UpdateUserRequestDTO.class)))
                .thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(put("/api/v1/users/99")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar status 409 Conflict ao tentar atualizar com e-mail já utilizado por outro usuário")
    void shouldReturnConflictWhenUpdatingWithAlreadyUsedEmail() throws Exception {
        UpdateUserRequestDTO updateDTO = new UpdateUserRequestDTO("Nome", "outro.usuario@test.com");

        when(userService.updateUserProfile(eq(1L), any(UpdateUserRequestDTO.class)))
                .thenThrow(new EmailAlreadyExistsException("outro.usuario@test.com"));

        mockMvc.perform(put("/api/v1/users/1")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Já existe um usuário cadastrado com o e-mail 'outro.usuario@test.com'"));
    }

    // --- DELETE TESTS ---

    @Test
    @DisplayName("Deve retornar status 401 Unauthorized ao tentar deletar usuário sem autenticação")
    void shouldReturnUnauthorizedWhenDeletingUserWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1")
                        .contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve deletar usuário por ID com sucesso e retornar status 204 No Content")
    void shouldDeleteUserByIdSuccessfully() throws Exception {
        doNothing().when(userService).deleteUserById(1L);

        mockMvc.perform(delete("/api/v1/users/1")
                        .contextPath("/api/v1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar status 404 Not Found ao tentar deletar usuário inexistente")
    void shouldReturnNotFoundWhenDeletingNonExistentUser() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).deleteUserById(99L);

        mockMvc.perform(delete("/api/v1/users/99")
                        .contextPath("/api/v1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

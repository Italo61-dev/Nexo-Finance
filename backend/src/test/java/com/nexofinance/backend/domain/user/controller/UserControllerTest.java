package com.nexofinance.backend.domain.user.controller;

import com.nexofinance.backend.domain.user.UserService;
import com.nexofinance.backend.domain.user.dto.RegisterUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UserResponseDTO;
import com.nexofinance.backend.domain.user.exception.EmailAlreadyExistsException;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("Deve cadastrar novo usuário com sucesso e retornar status 201 Created com header Location")
    void shouldCreateNewUserAccountSuccessfully() throws Exception {
        RegisterUserRequestDTO requestDTO = new RegisterUserRequestDTO(
                "Ítalo Sousa",
                "italo@nexofinance.com",
                "password123"
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
    @DisplayName("Deve retornar status 400 Bad Request quando a validação dos campos obrigatórios falhar")
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
                "password123"
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
}

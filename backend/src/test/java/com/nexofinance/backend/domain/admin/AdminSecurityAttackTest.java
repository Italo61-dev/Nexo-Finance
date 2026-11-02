package com.nexofinance.backend.domain.admin;

import com.nexofinance.backend.domain.admin.dto.UpdateUserRoleRequestDTO;
import com.nexofinance.backend.domain.admin.dto.UpdateUserStatusRequestDTO;
import com.nexofinance.backend.domain.user.AuthProvider;
import com.nexofinance.backend.domain.user.Role;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityAttackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("[Ataque 1] Usuário anônimo tenta acessar endpoints administrativos -> Deve retornar 401 Unauthorized")
    void attack1_unauthenticatedUserAccessingAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/users/metrics")
                        .contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "comum@nexofinance.com", roles = "USER")
    @DisplayName("[Ataque 2] Usuário comum (ROLE_USER) tenta escalada de privilégios ou acesso a rotas admin -> Deve retornar 403 Forbidden")
    void attack2_regularUserPrivilegeEscalationAttempt() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contextPath("/api/v1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(get("/api/v1/admin/users/metrics")
                        .contextPath("/api/v1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRoleRequestDTO(Role.ROLE_ADMIN))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDTO(false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 3] Auto-rebaixamento (Self-Demotion Attack) -> Administrador tenta retirar o próprio privilégio de admin")
    void attack3_adminAttemptsSelfDemotion() throws Exception {
        User adminUser = User.builder()
                .id(1L)
                .name("Admin Mestre")
                .email("admin@nexofinance.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRoleRequestDTO(Role.ROLE_USER))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Administradores não podem alterar o próprio perfil de acesso."));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 4] Auto-bloqueio (Self-Deactivation Attack) -> Administrador tenta desativar a própria conta")
    void attack4_adminAttemptsSelfDeactivation() throws Exception {
        User adminUser = User.builder()
                .id(1L)
                .name("Admin Mestre")
                .email("admin@nexofinance.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDTO(false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Administradores não podem desativar a própria conta."));
    }

    @Test
    @WithMockUser(username = "admin1@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 5] Bloqueio do último administrador (Sole Admin Demotion Attack) -> Bloqueia rebaixamento do único admin ativo")
    void attack5_attemptToDemoteSoleActiveAdmin() throws Exception {
        User targetAdmin = User.builder()
                .id(2L)
                .name("Segundo Admin")
                .email("admin2@nexofinance.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        // Simula que apenas 1 administrador ativo resta no sistema
        when(userRepository.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(1L);

        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRoleRequestDTO(Role.ROLE_USER))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Não é permitido rebaixar o único administrador ativo da plataforma."));
    }

    @Test
    @WithMockUser(username = "admin1@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 6] Desativação do último administrador (Sole Admin Deactivation Attack) -> Bloqueia desativação do único admin ativo")
    void attack6_attemptToDeactivateSoleActiveAdmin() throws Exception {
        User targetAdmin = User.builder()
                .id(2L)
                .name("Segundo Admin")
                .email("admin2@nexofinance.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        // Simula que apenas 1 administrador ativo resta no sistema
        when(userRepository.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(1L);

        mockMvc.perform(patch("/api/v1/admin/users/2/status")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDTO(false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Não é permitido desativar o único administrador ativo da plataforma."));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 7] Sort Injection / Probing de campo sensível (passwordHash) -> Deve retornar 400 Bad Request, nunca 500")
    void attack7_sortInjectionSensitiveFieldProbing() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contextPath("/api/v1")
                        .param("sort", "passwordHash,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("passwordHash")));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 8] Sort Injection com propriedade arbitrária inexistente -> Deve retornar 400 Bad Request")
    void attack8_sortInjectionArbitraryField() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contextPath("/api/v1")
                        .param("sort", "nonExistentField,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("nonExistentField")));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 9] Adulteração de Payload com Enum inválido -> Deve retornar 400 Bad Request (ProblemDetail)")
    void attack9_payloadTamperingInvalidRoleEnum() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPER_HACKER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido ou malformado"));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 10] Adulteração com campos nulos obrigatórios -> Deve retornar 400 com lista de erros de validação")
    void attack10_payloadTamperingNullFieldValidation() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors[0].field").value("role"));

        mockMvc.perform(patch("/api/v1/admin/users/2/status")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors[0].field").value("active"));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 11] SQL LIKE Wildcard Probing (caracteres %, _, \\) -> Deve sanitizar e executar 200 OK com segurança")
    void attack11_sqlLikeWildcardSanitization() throws Exception {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/users")
                        .contextPath("/api/v1")
                        .param("search", "%_\\"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("[Ataque 12] Alvo Inexistente (Target User IDOR/Probing) -> Deve retornar 404 Not Found")
    void attack12_targetUserNotFound() throws Exception {
        when(userRepository.findById(99999L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/admin/users/99999/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRoleRequestDTO(Role.ROLE_ADMIN))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}

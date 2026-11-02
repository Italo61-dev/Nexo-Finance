package com.nexofinance.backend.domain.admin.controller;

import com.nexofinance.backend.domain.admin.AdminUserService;
import com.nexofinance.backend.domain.admin.dto.AdminUserSummaryResponseDTO;
import com.nexofinance.backend.domain.admin.dto.PlatformMetricsResponseDTO;
import com.nexofinance.backend.domain.admin.dto.UpdateUserRoleRequestDTO;
import com.nexofinance.backend.domain.admin.dto.UpdateUserStatusRequestDTO;
import com.nexofinance.backend.domain.admin.exception.SelfModificationException;
import com.nexofinance.backend.domain.user.AuthProvider;
import com.nexofinance.backend.domain.user.Role;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    @DisplayName("Deve retornar 401 Unauthorized ao tentar listar usuários admin sem autenticação")
    void shouldReturnUnauthorizedWhenListingWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Deve retornar 403 Forbidden ao tentar listar usuários como usuário comum (ROLE_USER)")
    void shouldReturnForbiddenWhenUserRoleTriesToList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contextPath("/api/v1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar 200 OK com lista paginada de usuários quando admin")
    void shouldReturnUsersPageSuccessfullyWhenAdmin() throws Exception {
        AdminUserSummaryResponseDTO summaryDTO = new AdminUserSummaryResponseDTO(
                1L,
                "Usuário Teste",
                "teste@nexofinance.com",
                AuthProvider.LOCAL,
                Role.ROLE_USER,
                true,
                LocalDateTime.now()
        );

        when(adminUserService.findAllUsers(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summaryDTO)));

        mockMvc.perform(get("/api/v1/admin/users")
                        .contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Usuário Teste"))
                .andExpect(jsonPath("$.content[0].email").value("teste@nexofinance.com"))
                .andExpect(jsonPath("$.content[0].role").value("ROLE_USER"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar 200 OK com métricas consolidadas da plataforma")
    void shouldReturnPlatformMetricsSuccessfullyWhenAdmin() throws Exception {
        PlatformMetricsResponseDTO metricsDTO = new PlatformMetricsResponseDTO(
                100L,
                3L,
                75.5,
                4L
        );

        when(adminUserService.getPlatformMetrics()).thenReturn(metricsDTO);

        mockMvc.perform(get("/api/v1/admin/users/metrics")
                        .contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100L))
                .andExpect(jsonPath("$.totalAdmins").value(3L))
                .andExpect(jsonPath("$.googleAuthPercent").value(75.5))
                .andExpect(jsonPath("$.inactiveUsers").value(4L));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("Deve atualizar role de usuário com sucesso retornando 200 OK (PATCH)")
    void shouldUpdateUserRoleSuccessfullyViaPatch() throws Exception {
        UpdateUserRoleRequestDTO requestDTO = new UpdateUserRoleRequestDTO(Role.ROLE_ADMIN);
        AdminUserSummaryResponseDTO responseDTO = new AdminUserSummaryResponseDTO(
                2L,
                "Membro Promovido",
                "membro@nexofinance.com",
                AuthProvider.LOCAL,
                Role.ROLE_ADMIN,
                true,
                LocalDateTime.now()
        );

        when(adminUserService.updateRole(eq(2L), eq(Role.ROLE_ADMIN), eq("admin@nexofinance.com")))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("Deve atualizar role de usuário com sucesso retornando 200 OK (PUT)")
    void shouldUpdateUserRoleSuccessfullyViaPut() throws Exception {
        UpdateUserRoleRequestDTO requestDTO = new UpdateUserRoleRequestDTO(Role.ROLE_ADMIN);
        AdminUserSummaryResponseDTO responseDTO = new AdminUserSummaryResponseDTO(
                2L,
                "Membro Promovido",
                "membro@nexofinance.com",
                AuthProvider.LOCAL,
                Role.ROLE_ADMIN,
                true,
                LocalDateTime.now()
        );

        when(adminUserService.updateRole(eq(2L), eq(Role.ROLE_ADMIN), eq("admin@nexofinance.com")))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/admin/users/2/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("Deve retornar 400 Bad Request ao tentar alterar o próprio perfil de acesso")
    void shouldReturnBadRequestWhenAdminModifiesOwnRole() throws Exception {
        UpdateUserRoleRequestDTO requestDTO = new UpdateUserRoleRequestDTO(Role.ROLE_USER);

        when(adminUserService.updateRole(eq(1L), eq(Role.ROLE_USER), eq("admin@nexofinance.com")))
                .thenThrow(new SelfModificationException("Administradores não podem alterar o próprio perfil de acesso."));

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Administradores não podem alterar o próprio perfil de acesso."));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("Deve atualizar status de usuário com sucesso retornando 200 OK (PATCH)")
    void shouldUpdateUserStatusSuccessfullyViaPatch() throws Exception {
        UpdateUserStatusRequestDTO requestDTO = new UpdateUserStatusRequestDTO(false);
        AdminUserSummaryResponseDTO responseDTO = new AdminUserSummaryResponseDTO(
                2L,
                "Usuário Bloqueado",
                "bloqueado@nexofinance.com",
                AuthProvider.LOCAL,
                Role.ROLE_USER,
                false,
                LocalDateTime.now()
        );

        when(adminUserService.updateStatus(eq(2L), eq(false), eq("admin@nexofinance.com")))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/api/v1/admin/users/2/status")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(username = "admin@nexofinance.com", roles = "ADMIN")
    @DisplayName("Deve retornar 400 Bad Request ao tentar desativar a própria conta")
    void shouldReturnBadRequestWhenAdminDeactivatesSelf() throws Exception {
        UpdateUserStatusRequestDTO requestDTO = new UpdateUserStatusRequestDTO(false);

        when(adminUserService.updateStatus(eq(1L), eq(false), eq("admin@nexofinance.com")))
                .thenThrow(new SelfModificationException("Administradores não podem desativar a própria conta."));

        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Administradores não podem desativar a própria conta."));
    }
}

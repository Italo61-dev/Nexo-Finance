package com.nexofinance.backend.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.nexofinance.backend.domain.auth.TokenService;
import com.nexofinance.backend.domain.user.Role;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @TestConfiguration
    static class TestAdminControllerConfig {
        @RestController
        @RequestMapping("/admin/test-rbac")
        static class TestAdminController {
            @GetMapping
            @PreAuthorize("hasRole('ADMIN')")
            public String adminOnlyEndpoint() {
                return "Admin Access Granted";
            }
        }
    }

    @AfterEach
    void tearDown() {
        userRepository.findByEmail("user-rbac@test.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("admin-rbac@test.com").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized ao tentar acessar endpoint administrativo sem autenticação")
    void shouldReturnUnauthorizedWhenRequestingAdminEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado possui apenas perfil ROLE_USER")
    void shouldReturnForbiddenWhenUserHasOnlyUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acesso negado: você não possui permissão para acessar este recurso"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar 200 OK quando usuário autenticado possui perfil ROLE_ADMIN")
    void shouldReturnOkWhenUserHasAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin Access Granted"));
    }

    @Test
    @DisplayName("Deve barrar usuário com 403 Forbidden usando token JWT real de ROLE_USER")
    void shouldReturnForbiddenUsingRealJwtWithRoleUser() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Regular User")
                .email("user-rbac@test.com")
                .passwordHash("hash")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        String token = tokenService.generateToken(user);

        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Deve permitir acesso com 200 OK usando token JWT real de ROLE_ADMIN")
    void shouldReturnOkUsingRealJwtWithRoleAdmin() throws Exception {
        User admin = userRepository.save(User.builder()
                .name("Administrator")
                .email("admin-rbac@test.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        String token = tokenService.generateToken(admin);

        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin Access Granted"));
    }

    // --- SUÍTE DE TESTES DE ATAQUE & RESILIÊNCIA DE SEGURANÇA ---

    @Test
    @DisplayName("[ATAQUE] Deve barrar tentativa de escalação de privilégio com token forjado com chave arbitrária")
    void shouldBlockPrivilegeEscalationWithForgedSignatureToken() throws Exception {
        String forgedToken = JWT.create()
                .withIssuer("nexo-finance")
                .withSubject("user-rbac@test.com")
                .withClaim("role", "ROLE_ADMIN")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256("chave_completamente_falsa_e_arbitraria"));

        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[ATAQUE] Deve barrar token sem algoritmo de assinatura ('none' algorithm attack)")
    void shouldBlockNoneAlgorithmTokenAttack() throws Exception {
        String noneAlgToken = JWT.create()
                .withIssuer("nexo-finance")
                .withSubject("admin-rbac@test.com")
                .withClaim("role", "ROLE_ADMIN")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.none());

        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + noneAlgToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[ATAQUE] Deve barrar ataque de replay com token expirado")
    void shouldBlockReplayAttackWithExpiredToken() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Admin Expired")
                .email("admin-rbac@test.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        String expiredToken = JWT.create()
                .withIssuer("nexo-finance")
                .withSubject(user.getEmail())
                .withClaim("role", "ROLE_ADMIN")
                .withIssuedAt(Instant.now().minusSeconds(7200))
                .withExpiresAt(Instant.now().minusSeconds(3600))
                .sign(Algorithm.HMAC256("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"));

        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[ATAQUE] Deve barrar acesso imediato de usuário desativado (Immediate Lockout Attack)")
    void shouldBlockImmediateAccessWhenUserIsDeactivatedEvenWithValidToken() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Active Turned Inactive")
                .email("user-rbac@test.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        String validToken = tokenService.generateToken(user);

        // Desativação do usuário por um administrador
        user.setActive(false);
        userRepository.save(user);

        // O usuário tenta usar o token que ainda não expirou
        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[ATAQUE] Deve revogar permissões imediatamente quando admin é rebaixado a user comum")
    void shouldRevokePrivilegesImmediatelyWhenAdminIsDemotedToUser() throws Exception {
        User admin = userRepository.save(User.builder()
                .name("Former Admin")
                .email("admin-rbac@test.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        // Token gerado quando o usuário ainda era ADMIN (claim role = ROLE_ADMIN)
        String adminToken = tokenService.generateToken(admin);

        // Usuário é rebaixado no banco para ROLE_USER
        admin.setRole(Role.ROLE_USER);
        userRepository.save(admin);

        // Tentativa de acessar endpoint restrito com o token antigo que diz "ROLE_ADMIN"
        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("[ATAQUE] Deve rejeitar requisições com headers Authorization malformados sem causar erro 500")
    void shouldHandleMalformedAuthorizationHeadersGracefully() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/test-rbac")
                        .contextPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }
}

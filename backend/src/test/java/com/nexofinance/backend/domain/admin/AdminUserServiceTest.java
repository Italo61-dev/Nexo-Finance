package com.nexofinance.backend.domain.admin;

import com.nexofinance.backend.domain.admin.dto.AdminUserSummaryResponseDTO;
import com.nexofinance.backend.domain.admin.dto.PlatformMetricsResponseDTO;
import com.nexofinance.backend.domain.admin.exception.SelfModificationException;
import com.nexofinance.backend.domain.user.AuthProvider;
import com.nexofinance.backend.domain.user.Role;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import com.nexofinance.backend.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("Deve listar usuários com paginação e filtros")
    void shouldFindAllUsersWithPaginationAndFilters() {
        User user = User.builder()
                .id(1L)
                .name("Carlos Silva")
                .email("carlos@test.com")
                .role(Role.ROLE_USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AdminUserSummaryResponseDTO> result = adminUserService.findAllUsers("Carlos", Role.ROLE_USER, true, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Carlos Silva");
        assertThat(result.getContent().get(0).email()).isEqualTo("carlos@test.com");
    }

    @Test
    @DisplayName("Deve limitar o tamanho de página ao limite de 100 registros (Proteção contra DoS)")
    void shouldCapPageSizeToMaximumLimit() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adminUserService.findAllUsers(null, null, null, PageRequest.of(0, 500));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), captor.capture());

        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao tentar ordenar por campo não permitido")
    void shouldThrowIllegalArgumentExceptionWhenSortPropertyIsDisallowed() {
        Pageable unsafePageable = PageRequest.of(0, 10, Sort.by("passwordHash"));

        assertThatThrownBy(() -> adminUserService.findAllUsers(null, null, null, unsafePageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Campo de ordenação não permitido: 'passwordHash'");

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve atualizar o perfil de acesso (Role) com sucesso quando não for auto-alteração")
    void shouldUpdateUserRoleSuccessfully() {
        User target = User.builder()
                .id(2L)
                .name("Maria Santos")
                .email("maria@test.com")
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryResponseDTO response = adminUserService.updateRole(2L, Role.ROLE_ADMIN, "admin@test.com");

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(Role.ROLE_ADMIN);
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("Deve rebaixar perfil de admin com sucesso quando houver múltiplos administradores ativos")
    void shouldDemoteAdminSuccessfullyWhenMultipleAdminsExist() {
        User targetAdmin = User.builder()
                .id(2L)
                .name("Outro Admin")
                .email("outro@test.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryResponseDTO response = adminUserService.updateRole(2L, Role.ROLE_USER, "admin@test.com");

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(Role.ROLE_USER);
        verify(userRepository).save(targetAdmin);
    }

    @Test
    @DisplayName("Deve impedir rebaixamento do último administrador ativo da plataforma")
    void shouldThrowSelfModificationExceptionWhenAttemptingToDemoteLastActiveAdmin() {
        User targetAdmin = User.builder()
                .id(2L)
                .name("Único Admin")
                .email("outro@test.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.updateRole(2L, Role.ROLE_USER, "admin@test.com"))
                .isInstanceOf(SelfModificationException.class)
                .hasMessage("Não é permitido rebaixar o único administrador ativo da plataforma.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar SelfModificationException ao tentar alterar o próprio perfil de acesso")
    void shouldThrowSelfModificationExceptionWhenAdminModifiesOwnRole() {
        User currentAdmin = User.builder()
                .id(1L)
                .name("Admin Principal")
                .email("admin@test.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(currentAdmin));

        assertThatThrownBy(() -> adminUserService.updateRole(1L, Role.ROLE_USER, "admin@test.com"))
                .isInstanceOf(SelfModificationException.class)
                .hasMessage("Administradores não podem alterar o próprio perfil de acesso.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException quando identificação do administrador for ausente ou vazia")
    void shouldThrowAccessDeniedExceptionWhenAdminEmailIsMissingOrBlank() {
        assertThatThrownBy(() -> adminUserService.updateRole(1L, Role.ROLE_USER, "   "))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Operação administrativa requer identificação do administrador autenticado.");

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Deve atualizar status de ativação com sucesso quando não for auto-bloqueio")
    void shouldUpdateUserStatusSuccessfully() {
        User target = User.builder()
                .id(2L)
                .name("João Silva")
                .email("joao@test.com")
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryResponseDTO response = adminUserService.updateStatus(2L, false, "admin@test.com");

        assertThat(response).isNotNull();
        assertThat(response.active()).isFalse();
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("Deve impedir desativação do último administrador ativo da plataforma")
    void shouldThrowSelfModificationExceptionWhenAttemptingToDeactivateLastActiveAdmin() {
        User targetAdmin = User.builder()
                .id(2L)
                .name("Outro Admin")
                .email("outro@test.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.updateStatus(2L, false, "admin@test.com"))
                .isInstanceOf(SelfModificationException.class)
                .hasMessage("Não é permitido desativar o único administrador ativo da plataforma.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar SelfModificationException ao tentar desativar a própria conta")
    void shouldThrowSelfModificationExceptionWhenAdminDeactivatesSelf() {
        User currentAdmin = User.builder()
                .id(1L)
                .name("Admin Principal")
                .email("admin@test.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(currentAdmin));

        assertThatThrownBy(() -> adminUserService.updateStatus(1L, false, "admin@test.com"))
                .isInstanceOf(SelfModificationException.class)
                .hasMessage("Administradores não podem desativar a própria conta.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException ao tentar alterar usuário inexistente")
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.updateRole(999L, Role.ROLE_ADMIN, "admin@test.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado para o ID: 999");
    }

    @Test
    @DisplayName("Deve calcular e consolidar as métricas da plataforma corretamente")
    void shouldConsolidatePlatformMetricsAccurately() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(5L);
        when(userRepository.countByAuthProvider(AuthProvider.GOOGLE)).thenReturn(40L);
        when(userRepository.countByActive(false)).thenReturn(8L);

        PlatformMetricsResponseDTO metrics = adminUserService.getPlatformMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.totalUsers()).isEqualTo(100L);
        assertThat(metrics.totalAdmins()).isEqualTo(5L);
        assertThat(metrics.googleAuthPercent()).isEqualTo(40.0);
        assertThat(metrics.inactiveUsers()).isEqualTo(8L);
    }

    @Test
    @DisplayName("Deve retornar percentual 0.0 quando a plataforma ainda não tiver usuários cadastrados")
    void shouldReturnZeroPercentWhenPlatformHasNoUsers() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(0L);
        when(userRepository.countByAuthProvider(AuthProvider.GOOGLE)).thenReturn(0L);
        when(userRepository.countByActive(false)).thenReturn(0L);

        PlatformMetricsResponseDTO metrics = adminUserService.getPlatformMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.totalUsers()).isEqualTo(0L);
        assertThat(metrics.googleAuthPercent()).isEqualTo(0.0);
    }
}

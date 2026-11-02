package com.nexofinance.backend.domain.admin;

import com.nexofinance.backend.domain.admin.dto.AdminUserSummaryResponseDTO;
import com.nexofinance.backend.domain.admin.dto.PlatformMetricsResponseDTO;
import com.nexofinance.backend.domain.admin.exception.SelfModificationException;
import com.nexofinance.backend.domain.user.AuthProvider;
import com.nexofinance.backend.domain.user.Role;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import com.nexofinance.backend.domain.user.exception.UserNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "name", "email", "role", "active", "createdAt", "updatedAt"
    );

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponseDTO> findAllUsers(String search, Role role, Boolean active, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);

        Specification<User> userSpecification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isBlank()) {
                String sanitizedSearch = escapeLikePattern(search.trim().toLowerCase());
                String pattern = "%" + sanitizedSearch + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern, '\\')
                ));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(userSpecification, safePageable).map(AdminUserSummaryResponseDTO::fromEntity);
    }

    @Transactional
    public AdminUserSummaryResponseDTO updateRole(Long userId, Role newRole, User currentAdmin) {
        return updateRole(userId, newRole, currentAdmin != null ? currentAdmin.getEmail() : null);
    }

    @Transactional
    public AdminUserSummaryResponseDTO updateRole(Long userId, Role newRole, String currentAdminEmail) {
        validateAdminIdentity(currentAdminEmail);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String normalizedAdminEmail = currentAdminEmail.trim();
        if (targetUser.getEmail().trim().equalsIgnoreCase(normalizedAdminEmail)) {
            log.warn("Tentativa bloqueada: o administrador '{}' tentou alterar o próprio perfil de acesso.", normalizedAdminEmail);
            throw new SelfModificationException("Administradores não podem alterar o próprio perfil de acesso.");
        }

        // Proteção contra rebaixamento do último administrador ativo da plataforma
        if (targetUser.getRole() == Role.ROLE_ADMIN && newRole != Role.ROLE_ADMIN) {
            long activeAdmins = userRepository.countByRoleAndActive(Role.ROLE_ADMIN, true);
            if (activeAdmins <= 1) {
                log.warn("Tentativa bloqueada: tentativa de rebaixar o único administrador ativo do sistema por '{}'.", normalizedAdminEmail);
                throw new SelfModificationException("Não é permitido rebaixar o único administrador ativo da plataforma.");
            }
        }

        log.info("Perfil de acesso do usuário ID {} ({}) atualizado de {} para {} por '{}'.",
                targetUser.getId(), targetUser.getEmail(), targetUser.getRole(), newRole, normalizedAdminEmail);

        targetUser.setRole(newRole);
        User updatedUser = userRepository.save(targetUser);
        return AdminUserSummaryResponseDTO.fromEntity(updatedUser);
    }

    @Transactional
    public AdminUserSummaryResponseDTO updateStatus(Long userId, Boolean active, User currentAdmin) {
        return updateStatus(userId, active, currentAdmin != null ? currentAdmin.getEmail() : null);
    }

    @Transactional
    public AdminUserSummaryResponseDTO updateStatus(Long userId, Boolean active, String currentAdminEmail) {
        validateAdminIdentity(currentAdminEmail);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String normalizedAdminEmail = currentAdminEmail.trim();
        if (targetUser.getEmail().trim().equalsIgnoreCase(normalizedAdminEmail)) {
            log.warn("Tentativa bloqueada: o administrador '{}' tentou alterar o próprio status de ativação.", normalizedAdminEmail);
            throw new SelfModificationException("Administradores não podem desativar a própria conta.");
        }

        // Proteção contra desativação do último administrador ativo da plataforma
        if (targetUser.getRole() == Role.ROLE_ADMIN && Boolean.FALSE.equals(active)) {
            long activeAdmins = userRepository.countByRoleAndActive(Role.ROLE_ADMIN, true);
            if (activeAdmins <= 1) {
                log.warn("Tentativa bloqueada: tentativa de desativar o único administrador ativo do sistema por '{}'.", normalizedAdminEmail);
                throw new SelfModificationException("Não é permitido desativar o único administrador ativo da plataforma.");
            }
        }

        log.info("Status do usuário ID {} ({}) alterado de {} para {} por '{}'.",
                targetUser.getId(), targetUser.getEmail(), targetUser.getActive(), active, normalizedAdminEmail);

        targetUser.setActive(active);
        User updatedUser = userRepository.save(targetUser);
        return AdminUserSummaryResponseDTO.fromEntity(updatedUser);
    }

    @Transactional(readOnly = true)
    public PlatformMetricsResponseDTO getPlatformMetrics() {
        long totalUsers = userRepository.count();
        long totalAdmins = userRepository.countByRole(Role.ROLE_ADMIN);
        long googleUsers = userRepository.countByAuthProvider(AuthProvider.GOOGLE);
        long inactiveUsers = userRepository.countByActive(false);

        double googleAuthPercent = totalUsers > 0
                ? Math.round(((double) googleUsers / totalUsers) * 1000.0) / 10.0
                : 0.0;

        return new PlatformMetricsResponseDTO(
                totalUsers,
                totalAdmins,
                googleAuthPercent,
                inactiveUsers
        );
    }

    private void validateAdminIdentity(String adminEmail) {
        if (adminEmail == null || adminEmail.trim().isBlank()) {
            throw new AccessDeniedException("Operação administrativa requer identificação do administrador autenticado.");
        }
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                    throw new IllegalArgumentException(String.format(
                            "Campo de ordenação não permitido: '%s'. Campos permitidos: %s",
                            order.getProperty(), ALLOWED_SORT_PROPERTIES
                    ));
                }
            }
        }

        int boundedPageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        if (boundedPageSize != pageable.getPageSize()) {
            return PageRequest.of(pageable.getPageNumber(), boundedPageSize, pageable.getSort());
        }

        return pageable;
    }

    private String escapeLikePattern(String input) {
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

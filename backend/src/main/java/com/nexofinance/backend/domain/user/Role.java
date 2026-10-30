package com.nexofinance.backend.domain.user;

/**
 * Perfis de acesso do sistema Nexo Finance (RBAC - Role-Based Access Control).
 * Utiliza o prefixo padrão {@code ROLE_} esperado pelo Spring Security para anotações
 * como {@code @PreAuthorize("hasRole('ADMIN')")} ou verificações de authorities.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}

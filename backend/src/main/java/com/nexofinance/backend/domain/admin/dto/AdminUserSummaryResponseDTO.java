package com.nexofinance.backend.domain.admin.dto;

import com.nexofinance.backend.domain.user.AuthProvider;
import com.nexofinance.backend.domain.user.Role;
import com.nexofinance.backend.domain.user.User;

import java.time.LocalDateTime;

public record AdminUserSummaryResponseDTO(
        Long id,
        String name,
        String email,
        AuthProvider authProvider,
        Role role,
        Boolean active,
        LocalDateTime createdAt
) {
    public static AdminUserSummaryResponseDTO fromEntity(User user) {
        return new AdminUserSummaryResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAuthProvider() != null ? user.getAuthProvider() : AuthProvider.LOCAL,
                user.getRole() != null ? user.getRole() : Role.ROLE_USER,
                user.getActive(),
                user.getCreatedAt()
        );
    }
}

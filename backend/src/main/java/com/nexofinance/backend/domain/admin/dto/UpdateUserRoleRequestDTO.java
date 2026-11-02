package com.nexofinance.backend.domain.admin.dto;

import com.nexofinance.backend.domain.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequestDTO(
        @NotNull(message = "O perfil (role) é obrigatório")
        Role role
) {}

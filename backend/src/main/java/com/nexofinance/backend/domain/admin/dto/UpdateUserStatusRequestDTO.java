package com.nexofinance.backend.domain.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequestDTO(
        @NotNull(message = "O status de ativação (active) é obrigatório")
        Boolean active
) {}

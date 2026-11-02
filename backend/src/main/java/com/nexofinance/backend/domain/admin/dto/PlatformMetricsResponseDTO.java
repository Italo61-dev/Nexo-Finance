package com.nexofinance.backend.domain.admin.dto;

public record PlatformMetricsResponseDTO(
        Long totalUsers,
        Long totalAdmins,
        Double googleAuthPercent,
        Long inactiveUsers
) {}

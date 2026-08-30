package com.nexofinance.backend.domain.user.dto;

import com.nexofinance.backend.domain.user.User;

import java.time.LocalDateTime;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public UserResponseDTO(Long id, String name, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, email, true, createdAt, updatedAt);
    }

    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

package com.nexofinance.backend.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("Deve criar usuário utilizando Builder do Lombok e validar getters")
    void shouldCreateUserWithBuilder() {
        User user = User.builder()
                .name("João Silva")
                .email("joao.silva@example.com")
                .passwordHash("secure_hash_123")
                .build();

        assertThat(user.getName()).isEqualTo("João Silva");
        assertThat(user.getEmail()).isEqualTo("joao.silva@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("secure_hash_123");
    }

    @Test
    @DisplayName("Deve validar a integração com o contrato UserDetails do Spring Security")
    void shouldValidateUserDetailsIntegration() {
        User user = User.builder()
                .name("Maria Souza")
                .email("maria.souza@example.com")
                .passwordHash("secret_hash")
                .build();

        assertThat(user.getUsername()).isEqualTo("maria.souza@example.com");
        assertThat(user.getPassword()).isEqualTo("secret_hash");
        assertThat(user.getAuthorities()).hasSize(1);
        assertThat(user.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Deve preencher os timestamps no PrePersist e PreUpdate")
    void shouldPopulateTimestampsOnPrePersistAndPreUpdate() {
        User user = new User();
        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();

        LocalDateTime previousUpdate = user.getUpdatedAt();
        user.onUpdate();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(previousUpdate);
    }

    @Test
    @DisplayName("Deve validar equals e hashCode baseados no ID do usuário")
    void shouldValidateEqualsAndHashCode() {
        User u1 = User.builder().id(1L).name("User 1").email("u1@test.com").build();
        User u2 = User.builder().id(1L).name("User 2").email("u2@test.com").build();
        User u3 = User.builder().id(2L).name("User 3").email("u3@test.com").build();

        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
        assertThat(u1).isNotEqualTo(u3);
    }
}

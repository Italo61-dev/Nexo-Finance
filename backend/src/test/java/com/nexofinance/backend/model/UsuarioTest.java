package com.nexofinance.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    @DisplayName("Deve criar usuario com dados validos e validar campos")
    void deveCriarUsuarioComDadosValidos() {
        Usuario usuario = new Usuario("Ítalo", "italo@example.com", "hash123");

        assertThat(usuario.getNome()).isEqualTo("Ítalo");
        assertThat(usuario.getEmail()).isEqualTo("italo@example.com");
        assertThat(usuario.getSenhaHash()).isEqualTo("hash123");
    }

    @Test
    @DisplayName("Deve validar integracao com UserDetails do Spring Security")
    void deveValidarIntegracaoComUserDetails() {
        Usuario usuario = new Usuario("Ítalo", "italo@example.com", "hash123");

        assertThat(usuario.getUsername()).isEqualTo("italo@example.com");
        assertThat(usuario.getPassword()).isEqualTo("hash123");
        assertThat(usuario.getAuthorities()).hasSize(1);
        assertThat(usuario.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(usuario.isAccountNonExpired()).isTrue();
        assertThat(usuario.isAccountNonLocked()).isTrue();
        assertThat(usuario.isCredentialsNonExpired()).isTrue();
        assertThat(usuario.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Deve preencher timestamps no onCreate e onUpdate")
    void devePreencherTimestampsNoPrePersistEPreUpdate() {
        Usuario usuario = new Usuario();
        usuario.onCreate();

        assertThat(usuario.getCreatedAt()).isNotNull();
        assertThat(usuario.getUpdatedAt()).isNotNull();

        LocalDateTime previousUpdate = usuario.getUpdatedAt();
        usuario.onUpdate();
        assertThat(usuario.getUpdatedAt()).isAfterOrEqualTo(previousUpdate);
    }

    @Test
    @DisplayName("Deve validar equals e hashCode baseados no ID")
    void deveValidarEqualsEHashCode() {
        Usuario u1 = new Usuario(1L, "User 1", "user1@example.com", "pass", LocalDateTime.now(), LocalDateTime.now());
        Usuario u2 = new Usuario(1L, "User 2", "user2@example.com", "pass2", LocalDateTime.now(), LocalDateTime.now());
        Usuario u3 = new Usuario(2L, "User 3", "user3@example.com", "pass3", LocalDateTime.now(), LocalDateTime.now());

        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
        assertThat(u1).isNotEqualTo(u3);
    }
}

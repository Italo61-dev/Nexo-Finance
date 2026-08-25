package com.nexofinance.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("Should create user using Lombok builder and getters")
    void shouldCreateUserWithBuilder() {
        User user = User.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .passwordHash("secure_hash_123")
                .build();

        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("secure_hash_123");
    }

    @Test
    @DisplayName("Should validate Spring Security UserDetails contract")
    void shouldValidateUserDetailsIntegration() {
        User user = User.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .passwordHash("secret_hash")
                .build();

        assertThat(user.getUsername()).isEqualTo("jane.doe@example.com");
        assertThat(user.getPassword()).isEqualTo("secret_hash");
        assertThat(user.getAuthorities()).hasSize(1);
        assertThat(user.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should populate timestamps on PrePersist and PreUpdate")
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
    @DisplayName("Should validate equals and hashCode based on ID")
    void shouldValidateEqualsAndHashCode() {
        User u1 = User.builder().id(1L).name("User 1").email("u1@test.com").build();
        User u2 = User.builder().id(1L).name("User 2").email("u2@test.com").build();
        User u3 = User.builder().id(2L).name("User 3").email("u3@test.com").build();

        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
        assertThat(u1).isNotEqualTo(u3);
    }
}

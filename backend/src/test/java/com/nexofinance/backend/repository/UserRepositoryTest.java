package com.nexofinance.backend.repository;

import com.nexofinance.backend.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist and find user by email")
    void shouldSaveAndFindUserByEmail() {
        User user = User.builder()
                .name("Ítalo Sousa")
                .email("italo.sousa@nexofinance.com")
                .passwordHash("argon2_or_bcrypt_hash")
                .build();

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        Optional<User> foundUser = userRepository.findByEmail("italo.sousa@nexofinance.com");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("Ítalo Sousa");
        assertThat(foundUser.get().getPasswordHash()).isEqualTo("argon2_or_bcrypt_hash");
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckUserExistenceByEmail() {
        User user = User.builder()
                .name("Existing User")
                .email("existing@nexofinance.com")
                .passwordHash("hash_pass")
                .build();

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("existing@nexofinance.com");
        boolean doesNotExist = userRepository.existsByEmail("nonexistent@nexofinance.com");

        assertThat(exists).isTrue();
        assertThat(doesNotExist).isFalse();
    }
}

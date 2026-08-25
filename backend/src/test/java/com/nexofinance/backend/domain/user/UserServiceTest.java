package com.nexofinance.backend.domain.user;

import com.nexofinance.backend.domain.user.dto.RegisterUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UserResponseDTO;
import com.nexofinance.backend.domain.user.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Deve cadastrar um novo usuário com sucesso e senha criptografada")
    void shouldRegisterNewUserSuccessfully() {
        RegisterUserRequestDTO requestDTO = new RegisterUserRequestDTO(
                "Ítalo Sousa",
                "Italo@Example.com",
                "password123"
        );

        when(userRepository.existsByEmail("italo@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_hash");

        User savedUser = User.builder()
                .id(1L)
                .name("Ítalo Sousa")
                .email("italo@example.com")
                .passwordHash("encoded_hash")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponseDTO response = userService.register(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ítalo Sousa");
        assertThat(response.email()).isEqualTo("italo@example.com");

        verify(userRepository).existsByEmail("italo@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar EmailAlreadyExistsException quando o e-mail já estiver cadastrado")
    void shouldThrowExceptionWhenEmailExists() {
        RegisterUserRequestDTO requestDTO = new RegisterUserRequestDTO(
                "Existing User",
                "existing@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(requestDTO))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Já existe um usuário cadastrado com o e-mail 'existing@example.com'");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}

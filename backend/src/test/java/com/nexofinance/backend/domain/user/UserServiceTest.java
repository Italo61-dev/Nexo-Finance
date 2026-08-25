package com.nexofinance.backend.domain.user;

import com.nexofinance.backend.domain.user.dto.RegisterUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UpdateUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UserResponseDTO;
import com.nexofinance.backend.domain.user.exception.EmailAlreadyExistsException;
import com.nexofinance.backend.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    void shouldThrowExceptionWhenEmailExistsOnRegistration() {
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

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void shouldFindUserByIdSuccessfully() {
        User user = User.builder()
                .id(1L)
                .name("Ítalo Sousa")
                .email("italo@nexofinance.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDTO response = userService.findUserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ítalo Sousa");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException quando usuário não for encontrado por ID")
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado para o ID: 99");

        verify(userRepository).findById(99L);
    }

    @Test
    @DisplayName("Deve listar todos os usuários de forma paginada")
    void shouldFindAllUsersPaginated() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = User.builder()
                .id(1L)
                .name("Ítalo")
                .email("italo@test.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserResponseDTO> result = userService.findAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Ítalo");
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Deve atualizar o perfil do usuário com sucesso")
    void shouldUpdateUserProfileSuccessfully() {
        UpdateUserRequestDTO updateDTO = new UpdateUserRequestDTO("Novo Nome", "novo.email@test.com");

        User existingUser = User.builder()
                .id(1L)
                .name("Nome Antigo")
                .email("antigo@test.com")
                .passwordHash("hash")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("novo.email@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        UserResponseDTO response = userService.updateUserProfile(1L, updateDTO);

        assertThat(response).isNotNull();
        assertThat(existingUser.getName()).isEqualTo("Novo Nome");
        assertThat(existingUser.getEmail()).isEqualTo("novo.email@test.com");
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Deve lançar EmailAlreadyExistsException ao tentar atualizar com e-mail já utilizado por outro usuário")
    void shouldThrowExceptionWhenUpdatingWithAlreadyUsedEmail() {
        UpdateUserRequestDTO updateDTO = new UpdateUserRequestDTO("Nome", "outro.usuario@test.com");

        User existingUser = User.builder()
                .id(1L)
                .name("Meu Nome")
                .email("meu.email@test.com")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("outro.usuario@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserProfile(1L, updateDTO))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Já existe um usuário cadastrado com o e-mail 'outro.usuario@test.com'");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve deletar usuário por ID com sucesso")
    void shouldDeleteUserByIdSuccessfully() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUserById(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException ao tentar deletar usuário inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado para o ID: 99");

        verify(userRepository, never()).deleteById(any());
    }
}

package com.nexofinance.backend.domain.user;

import com.nexofinance.backend.domain.user.dto.RegisterUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UpdateUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UserResponseDTO;
import com.nexofinance.backend.domain.user.exception.EmailAlreadyExistsException;
import com.nexofinance.backend.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO register(RegisterUserRequestDTO requestDTO) {
        String email = requestDTO.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .name(requestDTO.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(requestDTO.password()))
                .build();

        User savedUser = userRepository.save(user);

        return UserResponseDTO.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return UserResponseDTO.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponseDTO::fromEntity);
    }

    @Transactional
    public UserResponseDTO updateUserProfile(Long id, UpdateUserRequestDTO requestDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        String newEmail = requestDTO.email().trim().toLowerCase();

        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException(newEmail);
        }

        user.setName(requestDTO.name().trim());
        user.setEmail(newEmail);

        User updatedUser = userRepository.save(user);

        return UserResponseDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }

        userRepository.deleteById(id);
    }
}

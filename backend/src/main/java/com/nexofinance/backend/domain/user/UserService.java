package com.nexofinance.backend.domain.user;

import com.nexofinance.backend.domain.user.dto.RegisterUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UserResponseDTO;
import com.nexofinance.backend.domain.user.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
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
        String normalizedEmail = requestDTO.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .name(requestDTO.name().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(requestDTO.password()))
                .build();

        User savedUser = userRepository.save(user);

        return UserResponseDTO.fromEntity(savedUser);
    }
}

package com.nexofinance.backend.domain.auth;

import com.nexofinance.backend.domain.auth.dto.AuthResponseDTO;
import com.nexofinance.backend.domain.auth.dto.ForgotPasswordRequestDTO;
import com.nexofinance.backend.domain.auth.dto.GoogleLoginRequestDTO;
import com.nexofinance.backend.domain.auth.dto.GoogleUserInfo;
import com.nexofinance.backend.domain.auth.dto.LoginRequestDTO;
import com.nexofinance.backend.domain.auth.dto.MessageResponseDTO;
import com.nexofinance.backend.domain.auth.dto.ResetPasswordRequestDTO;
import com.nexofinance.backend.domain.auth.exception.InvalidCredentialsException;
import com.nexofinance.backend.domain.auth.exception.InvalidPasswordException;
import com.nexofinance.backend.domain.auth.exception.InvalidTokenException;
import com.nexofinance.backend.domain.user.AuthProvider;
import com.nexofinance.backend.domain.user.User;
import com.nexofinance.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Transactional(readOnly = true)
    public AuthResponseDTO authenticate(LoginRequestDTO loginRequestDTO) {
        String email = loginRequestDTO.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPasswordHash() == null || !passwordEncoder.matches(loginRequestDTO.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenService.generateToken(user);

        return AuthResponseDTO.of(token, tokenService.getExpirationSeconds());
    }

    @Transactional
    public AuthResponseDTO authenticateWithGoogle(GoogleLoginRequestDTO requestDTO) {
        GoogleUserInfo googleUserInfo = googleTokenVerifierService.verify(requestDTO.idToken());

        String email = googleUserInfo.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email).map(existingUser -> {
            if (existingUser.getAuthProvider() == null) {
                existingUser.setAuthProvider(AuthProvider.GOOGLE);
            }
            if (existingUser.getProviderId() == null && googleUserInfo.sub() != null) {
                existingUser.setProviderId(googleUserInfo.sub());
            }
            return userRepository.save(existingUser);
        }).orElseGet(() -> {
            User newUser = User.builder()
                    .name(googleUserInfo.name() != null && !googleUserInfo.name().isBlank() ? googleUserInfo.name() : email)
                    .email(email)
                    .passwordHash(null)
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(googleUserInfo.sub())
                    .active(true)
                    .build();
            return userRepository.save(newUser);
        });

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidTokenException("A conta do usuário encontra-se inativa.");
        }

        String token = tokenService.generateToken(user);

        return AuthResponseDTO.of(token, tokenService.getExpirationSeconds());
    }

    @Transactional
    public MessageResponseDTO requestPasswordReset(ForgotPasswordRequestDTO requestDTO) {
        String email = requestDTO.email().trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getActive())) {
                String token = UUID.randomUUID().toString();
                LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(30);

                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .token(token)
                        .user(user)
                        .expiryDate(expiryDate)
                        .used(false)
                        .build();

                passwordResetTokenRepository.save(resetToken);
                emailService.sendPasswordResetEmail(user.getEmail(), token);
            }
        });

        return new MessageResponseDTO("Se o e-mail informado estiver cadastrado, as instruções para redefinição foram enviadas.");
    }

    @Transactional
    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO requestDTO) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(requestDTO.token())
                .orElseThrow(() -> new InvalidTokenException("Token de recuperação inválido ou inexistente."));

        if (!resetToken.isValid()) {
            if (resetToken.isExpired()) {
                throw new InvalidTokenException("O token de recuperação expirou. Solicite um novo link.");
            }
            if (Boolean.TRUE.equals(resetToken.getUsed())) {
                throw new InvalidTokenException("Este token de recuperação já foi utilizado.");
            }
            throw new InvalidTokenException("Token de recuperação inválido.");
        }

        User user = resetToken.getUser();
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidTokenException("A conta do usuário encontra-se inativa.");
        }

        if (passwordEncoder.matches(requestDTO.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("A nova senha não pode ser igual à senha anterior.");
        }

        user.setPasswordHash(passwordEncoder.encode(requestDTO.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return new MessageResponseDTO("Senha redefinida com sucesso. Você já pode fazer login com a nova senha.");
    }
}

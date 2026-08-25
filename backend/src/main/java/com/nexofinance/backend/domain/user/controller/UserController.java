package com.nexofinance.backend.domain.user.controller;

import com.nexofinance.backend.domain.user.UserService;
import com.nexofinance.backend.domain.user.dto.RegisterUserRequestDTO;
import com.nexofinance.backend.domain.user.dto.UserResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> createNewUserAccount(
            @Valid @RequestBody RegisterUserRequestDTO registerUserRequestDTO
    ) {
        UserResponseDTO createdUser = userService.register(registerUserRequestDTO);

        URI locationHeader = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.id())
                .toUri();

        return ResponseEntity.created(locationHeader).body(createdUser);
    }
}

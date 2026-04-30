package com.tu.backend.auth.service;

import com.tu.backend.auth.dto.AuthResponse;
import com.tu.backend.auth.dto.LoginRequest;
import com.tu.backend.auth.dto.RegisterRequest;
import com.tu.backend.auth.dto.UserDto;
import com.tu.backend.auth.entity.UserEntity;
import com.tu.backend.auth.repository.UserRepository;
import com.tu.backend.common.BusinessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(40020, "username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(40021, "email already exists");
        }

        UserEntity user = new UserEntity();
        user.setId("u-" + UUID.randomUUID().toString().replace("-", ""));
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(normalizeOptional(request.displayName()));

        return new AuthResponse(toDto(userRepository.save(user)), "none");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String account = normalizeAccount(request.account());
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(account, request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new BusinessException(40022, "invalid account or password");
        } catch (AuthenticationException ex) {
            throw new BusinessException(40022, "invalid account or password");
        }

        UserEntity user = userRepository.findByUsername(account)
            .or(() -> userRepository.findByEmail(account))
            .orElseThrow(() -> new BusinessException(40022, "invalid account or password"));
        return new AuthResponse(toDto(user), "none");
    }

    private UserDto toDto(UserEntity user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.getCreatedAt()
        );
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeAccount(String account) {
        return account.trim().toLowerCase();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

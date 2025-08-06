package com.devwonder.auth_service.service;

import com.devwonder.auth_service.model.User;
import com.devwonder.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;

    public Optional<User> authenticate(String username, String password) {
        log.debug("Attempting authentication for user: {}", username);
        
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            log.warn("Authentication failed: empty username or password");
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("Authentication failed: user not found: {}", username);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            log.warn("Authentication failed: user disabled: {}", username);
            return Optional.empty();
        }

        if (!userRepository.validatePassword(password, user.getPassword())) {
            log.warn("Authentication failed: invalid password for user: {}", username);
            return Optional.empty();
        }

        log.info("Authentication successful for user: {}", username);
        return Optional.of(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public void createUser(User user) {
        if (existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("User already exists: " + user.getUsername());
        }
        userRepository.save(user);
        log.info("User created successfully: {}", user.getUsername());
    }
}
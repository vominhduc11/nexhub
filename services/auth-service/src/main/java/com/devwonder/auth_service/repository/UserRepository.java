package com.devwonder.auth_service.repository;

import com.devwonder.auth_service.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    
    private final Map<String, User> users = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserRepository() {
        // Initialize with default users (in production, this would come from database)
        users.put("admin", new User("admin", passwordEncoder.encode("password123"), "ADMIN", true));
        users.put("user", new User("user", passwordEncoder.encode("userpass"), "USER", true));
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void save(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        users.put(user.getUsername(), user);
    }

    public void deleteByUsername(String username) {
        users.remove(username);
    }

    public boolean existsByUsername(String username) {
        return users.containsKey(username);
    }
}
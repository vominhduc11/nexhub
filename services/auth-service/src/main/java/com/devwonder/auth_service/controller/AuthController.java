package com.devwonder.auth_service.controller;

import com.devwonder.auth_service.constant.SecurityConstants;
import com.devwonder.auth_service.dto.LoginRequest;
import com.devwonder.auth_service.dto.LoginResponse;
import com.devwonder.auth_service.model.User;
import com.devwonder.auth_service.service.UserService;
import com.devwonder.auth_service.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        log.info("Login attempt for user: {} from IP: {}", loginRequest.getUsername(), clientIp);
        
        try {
            Optional<User> userOpt = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
            
            if (userOpt.isEmpty()) {
                log.warn("Login failed for user: {} from IP: {}", loginRequest.getUsername(), clientIp);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials", "timestamp", System.currentTimeMillis()));
            }
            
            User user = userOpt.get();
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            LoginResponse response = new LoginResponse(token, user.getUsername(), user.getRole());
            
            log.info("Login successful for user: {} from IP: {}", user.getUsername(), clientIp);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Login error for user: {} from IP: {}", loginRequest.getUsername(), clientIp, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Authentication service unavailable", "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test(HttpServletRequest request) {
        String userId = request.getHeader(SecurityConstants.USER_ID_HEADER);
        String userRole = request.getHeader(SecurityConstants.USER_ROLE_HEADER);
        
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "No authentication", "timestamp", System.currentTimeMillis()));
        }

        if (!SecurityConstants.ROLE_ADMIN.equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Admin role required", "timestamp", System.currentTimeMillis()));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Hello Admin! User: " + userId + " with role: " + userRole,
            "timestamp", System.currentTimeMillis(),
            "user", userId,
            "role", userRole
        ));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", username,
                    "role", role,
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Invalid or expired token"));
            }
        } catch (Exception e) {
            log.error("Token validation error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("valid", false, "error", "Token validation failed"));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

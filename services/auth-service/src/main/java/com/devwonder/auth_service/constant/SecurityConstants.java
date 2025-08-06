package com.devwonder.auth_service.constant;

public final class SecurityConstants {
    
    private SecurityConstants() {
        // Utility class - prevent instantiation
    }

    // JWT Configuration
    public static final int JWT_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours
    public static final String JWT_ALGORITHM = "RS256";
    public static final String JWT_KEY_ID = "nexhub-key-1";
    
    // Headers
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    
    // Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    
    // Rate Limiting
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes
}
package com.devwonder.auth_service.controller;

import com.devwonder.auth_service.config.RSAKeyProperties;
import com.devwonder.auth_service.constant.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class JwksController {
    private final RSAKeyProperties rsaKeys;

    public JwksController(RSAKeyProperties rsaKeys) {
        this.rsaKeys = rsaKeys;
    }

    @GetMapping("/auth/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        try {
            RSAPublicKey publicKey = rsaKeys.getPublicKey();
            
            Map<String, Object> jwk = createJwkFromPublicKey(publicKey);
            Map<String, Object> jwks = Map.of("keys", List.of(jwk));
            
            log.debug("JWKS endpoint accessed, returning public key with kid: {}", SecurityConstants.JWT_KEY_ID);
            
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1))) // Cache for 1 hour
                .body(jwks);
                
        } catch (Exception e) {
            log.error("Error generating JWKS response", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Unable to generate JWKS", "timestamp", System.currentTimeMillis()));
        }
    }

    private Map<String, Object> createJwkFromPublicKey(RSAPublicKey publicKey) {
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("kid", SecurityConstants.JWT_KEY_ID);
        jwk.put("alg", SecurityConstants.JWT_ALGORITHM);
        
        // Convert RSA components to Base64 URL-safe encoding
        jwk.put("n", base64UrlEncode(publicKey.getModulus()));
        jwk.put("e", base64UrlEncode(publicKey.getPublicExponent()));
        
        return jwk;
    }

    private String base64UrlEncode(BigInteger value) {
        if (value == null) {
            throw new IllegalArgumentException("BigInteger value cannot be null");
        }
        
        byte[] bytes = value.toByteArray();
        
        // Remove leading zero byte if present (for positive numbers)
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
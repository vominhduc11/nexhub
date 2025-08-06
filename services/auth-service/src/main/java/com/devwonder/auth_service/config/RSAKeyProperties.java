package com.devwonder.auth_service.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Getter
@Component
@ConfigurationProperties(prefix = "rsa")
public class RSAKeyProperties {
    private static final String PRIVATE_KEY_FILE = "private_key.pem";
    private static final String PUBLIC_KEY_FILE = "public_key.pem";
    private static final String RSA_ALGORITHM = "RSA";

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public RSAKeyProperties() {
        log.info("Initializing RSA key properties");
        
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            
            // Load and validate private key
            this.privateKey = loadPrivateKey(keyFactory);
            log.info("Private key loaded successfully");
            
            // Load and validate public key
            this.publicKey = loadPublicKey(keyFactory);
            log.info("Public key loaded successfully");
            
            // Validate key pair
            validateKeyPair();
            log.info("RSA key pair validation completed successfully");
            
        } catch (NoSuchAlgorithmException e) {
            log.error("RSA algorithm not available", e);
            throw new IllegalStateException("RSA algorithm not supported", e);
        } catch (Exception e) {
            log.error("Failed to initialize RSA keys", e);
            throw new IllegalStateException("RSA key initialization failed", e);
        }
    }

    private RSAPrivateKey loadPrivateKey(KeyFactory keyFactory) throws IOException, InvalidKeySpecException {
        String privateKeyContent = loadKeyFromFile(PRIVATE_KEY_FILE);
        String cleanedKey = cleanKeyContent(privateKeyContent, "PRIVATE KEY");
        
        byte[] privateKeyBytes = Base64.getDecoder().decode(cleanedKey);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        
        return (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
    }

    private RSAPublicKey loadPublicKey(KeyFactory keyFactory) throws IOException, InvalidKeySpecException {
        String publicKeyContent = loadKeyFromFile(PUBLIC_KEY_FILE);
        String cleanedKey = cleanKeyContent(publicKeyContent, "PUBLIC KEY");
        
        byte[] publicKeyBytes = Base64.getDecoder().decode(cleanedKey);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        
        return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
    }

    private String loadKeyFromFile(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource(filename);
        
        if (!resource.exists()) {
            throw new IOException("Key file not found: " + filename);
        }
        
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String cleanKeyContent(String keyContent, String keyType) {
        if (keyContent == null || keyContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Key content is null or empty");
        }
        
        return keyContent
            .replaceAll("-----BEGIN " + keyType + "-----", "")
            .replaceAll("-----END " + keyType + "-----", "")
            .replaceAll("\\s", "");
    }

    private void validateKeyPair() {
        if (privateKey == null) {
            throw new IllegalStateException("Private key is null");
        }
        if (publicKey == null) {
            throw new IllegalStateException("Public key is null");
        }
        
        // Validate key modulus match (basic validation)
        if (!privateKey.getModulus().equals(publicKey.getModulus())) {
            throw new IllegalStateException("Private and public key modulus do not match");
        }
        
        log.debug("Key pair validation successful - modulus length: {} bits", 
                    privateKey.getModulus().bitLength());
    }

}
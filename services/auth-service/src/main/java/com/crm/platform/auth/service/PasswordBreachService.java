package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

/**
 * Service for checking passwords against known breach databases
 * Uses HaveIBeenPwned API for breach detection
 */
@Service
public class PasswordBreachService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordBreachService.class);
    private static final String HIBP_API_URL = "https://api.pwnedpasswords.com/range/";

    private final RestTemplate restTemplate;

    @Value("${app.security.password.breach-check-enabled:true}")
    private boolean breachCheckEnabled;

    @Value("${app.security.password.breach-check-timeout:5000}")
    private int breachCheckTimeoutMs;

    public PasswordBreachService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Check if password has been compromised in known data breaches
     * Uses k-anonymity model - only sends first 5 characters of SHA-1 hash
     */
    public CompletableFuture<Boolean> isPasswordCompromised(String password) {
        if (!breachCheckEnabled) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String sha1Hash = sha1Hash(password).toUpperCase();
                String hashPrefix = sha1Hash.substring(0, 5);
                String hashSuffix = sha1Hash.substring(5);

                String response = restTemplate.getForObject(HIBP_API_URL + hashPrefix, String.class);
                
                if (response != null) {
                    String[] lines = response.split("\n");
                    for (String line : lines) {
                        String[] parts = line.split(":");
                        if (parts.length == 2 && parts[0].equals(hashSuffix)) {
                            int count = Integer.parseInt(parts[1].trim());
                            logger.warn("Password found in {} breaches", count);
                            return true;
                        }
                    }
                }
                
                return false;
            } catch (Exception e) {
                logger.warn("Error checking password breach status: {}", e.getMessage());
                // Fail open - don't block password changes due to API issues
                return false;
            }
        });
    }

    /**
     * Check if password is compromised with breach count
     */
    public CompletableFuture<BreachCheckResult> checkPasswordBreach(String password) {
        if (!breachCheckEnabled) {
            return CompletableFuture.completedFuture(new BreachCheckResult(false, 0));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String sha1Hash = sha1Hash(password).toUpperCase();
                String hashPrefix = sha1Hash.substring(0, 5);
                String hashSuffix = sha1Hash.substring(5);

                String response = restTemplate.getForObject(HIBP_API_URL + hashPrefix, String.class);
                
                if (response != null) {
                    String[] lines = response.split("\n");
                    for (String line : lines) {
                        String[] parts = line.split(":");
                        if (parts.length == 2 && parts[0].equals(hashSuffix)) {
                            int count = Integer.parseInt(parts[1].trim());
                            return new BreachCheckResult(true, count);
                        }
                    }
                }
                
                return new BreachCheckResult(false, 0);
            } catch (Exception e) {
                logger.warn("Error checking password breach status: {}", e.getMessage());
                return new BreachCheckResult(false, 0);
            }
        });
    }

    private String sha1Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Result of password breach check
     */
    public static class BreachCheckResult {
        private final boolean isCompromised;
        private final int breachCount;

        public BreachCheckResult(boolean isCompromised, int breachCount) {
            this.isCompromised = isCompromised;
            this.breachCount = breachCount;
        }

        public boolean isCompromised() { return isCompromised; }
        public int getBreachCount() { return breachCount; }
    }
}
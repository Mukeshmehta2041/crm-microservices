package com.crm.platform.security.config;

import com.crm.platform.security.encryption.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Manager for secure configuration properties with encryption support
 */
@Component
public class SecureConfigurationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureConfigurationManager.class);
    
    private static final String ENCRYPTED_PREFIX = "ENC(";
    private static final String ENCRYPTED_SUFFIX = ")";
    private static final Pattern ENCRYPTED_PATTERN = Pattern.compile("^ENC\\(.+\\)$");
    
    // Patterns for sensitive property names
    private static final Pattern[] SENSITIVE_PATTERNS = {
        Pattern.compile(".*password.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*secret.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*key.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*token.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*credential.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*api[_-]?key.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*private[_-]?key.*", Pattern.CASE_INSENSITIVE)
    };
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private EncryptionService encryptionService;
    
    private final Map<String, String> decryptedCache = new HashMap<>();
    
    /**
     * Get property value with automatic decryption if encrypted
     */
    public String getProperty(String key) {
        String value = environment.getProperty(key);
        
        if (value == null) {
            return null;
        }
        
        if (isEncrypted(value)) {
            return decryptProperty(key, value);
        }
        
        return value;
    }
    
    /**
     * Get property value with default
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Check if property value is encrypted
     */
    public boolean isEncrypted(String value) {
        return value != null && ENCRYPTED_PATTERN.matcher(value).matches();
    }
    
    /**
     * Check if property name indicates sensitive data
     */
    public boolean isSensitiveProperty(String propertyName) {
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(propertyName).matches()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Encrypt a property value for storage
     */
    public String encryptProperty(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        String encrypted = encryptionService.encrypt(value);
        return ENCRYPTED_PREFIX + encrypted + ENCRYPTED_SUFFIX;
    }
    
    /**
     * Decrypt an encrypted property value
     */
    private String decryptProperty(String key, String encryptedValue) {
        // Check cache first
        if (decryptedCache.containsKey(key)) {
            return decryptedCache.get(key);
        }
        
        try {
            // Remove ENC() wrapper
            String encryptedContent = encryptedValue.substring(
                ENCRYPTED_PREFIX.length(), 
                encryptedValue.length() - ENCRYPTED_SUFFIX.length()
            );
            
            String decrypted = encryptionService.decrypt(encryptedContent);
            
            // Cache decrypted value
            decryptedCache.put(key, decrypted);
            
            return decrypted;
        } catch (Exception e) {
            logger.error("Failed to decrypt property: {}", key, e);
            throw new SecureConfigurationException("Failed to decrypt property: " + key, e);
        }
    }
    
    /**
     * Get all properties with sensitive values masked
     */
    public Map<String, String> getAllPropertiesMasked() {
        Map<String, String> properties = new HashMap<>();
        
        for (String key : environment.getPropertySources().iterator().next().getPropertyNames()) {
            String value = environment.getProperty(key);
            
            if (isSensitiveProperty(key) || isEncrypted(value)) {
                properties.put(key, "***MASKED***");
            } else {
                properties.put(key, value);
            }
        }
        
        return properties;
    }
    
    /**
     * Validate that all required encrypted properties can be decrypted
     */
    public void validateEncryptedProperties() {
        logger.info("Validating encrypted properties...");
        
        int validatedCount = 0;
        int errorCount = 0;
        
        for (String key : environment.getPropertySources().iterator().next().getPropertyNames()) {
            String value = environment.getProperty(key);
            
            if (isEncrypted(value)) {
                try {
                    decryptProperty(key, value);
                    validatedCount++;
                    logger.debug("Successfully validated encrypted property: {}", key);
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Failed to validate encrypted property: {}", key, e);
                }
            }
        }
        
        logger.info("Encrypted property validation complete. Validated: {}, Errors: {}", 
                   validatedCount, errorCount);
        
        if (errorCount > 0) {
            throw new SecureConfigurationException(
                "Failed to validate " + errorCount + " encrypted properties");
        }
    }
    
    /**
     * Clear decryption cache
     */
    public void clearCache() {
        decryptedCache.clear();
        logger.debug("Cleared decrypted property cache");
    }
}
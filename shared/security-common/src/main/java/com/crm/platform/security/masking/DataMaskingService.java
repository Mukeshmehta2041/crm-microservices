package com.crm.platform.security.masking;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for masking sensitive data for testing and logging
 */
@Service
public class DataMaskingService {
    
    private static final String MASK_CHAR = "*";
    private static final String EMAIL_MASK = "***@***.***";
    private static final String PHONE_MASK = "***-***-****";
    private static final String CREDIT_CARD_MASK = "****-****-****-****";
    
    // Patterns for detecting sensitive data
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]?[0-9]{7,15}$"
    );
    
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "^[0-9]{13,19}$"
    );
    
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "^[0-9]{3}-?[0-9]{2}-?[0-9]{4}$"
    );
    
    /**
     * Mask email address
     */
    public String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return EMAIL_MASK;
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        
        String maskedLocal = maskString(localPart, 1, 1);
        String maskedDomain = maskDomain(domainPart);
        
        return maskedLocal + "@" + maskedDomain;
    }
    
    /**
     * Mask phone number
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        
        // Remove non-digits for pattern matching
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        
        if (!PHONE_PATTERN.matcher(digitsOnly).matches()) {
            return phone;
        }
        
        if (phone.length() <= 4) {
            return MASK_CHAR.repeat(phone.length());
        }
        
        // Show last 4 digits
        return MASK_CHAR.repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
    
    /**
     * Mask credit card number
     */
    public String maskCreditCard(String creditCard) {
        if (creditCard == null || creditCard.isEmpty()) {
            return creditCard;
        }
        
        String digitsOnly = creditCard.replaceAll("[^0-9]", "");
        
        if (!CREDIT_CARD_PATTERN.matcher(digitsOnly).matches()) {
            return creditCard;
        }
        
        if (creditCard.length() <= 4) {
            return MASK_CHAR.repeat(creditCard.length());
        }
        
        // Show last 4 digits
        return MASK_CHAR.repeat(creditCard.length() - 4) + creditCard.substring(creditCard.length() - 4);
    }
    
    /**
     * Mask Social Security Number
     */
    public String maskSSN(String ssn) {
        if (ssn == null || ssn.isEmpty()) {
            return ssn;
        }
        
        if (!SSN_PATTERN.matcher(ssn).matches()) {
            return ssn;
        }
        
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }
    
    /**
     * Generic string masking
     */
    public String maskString(String input, int prefixLength, int suffixLength) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        if (input.length() <= prefixLength + suffixLength) {
            return MASK_CHAR.repeat(input.length());
        }
        
        String prefix = input.substring(0, prefixLength);
        String suffix = input.substring(input.length() - suffixLength);
        String middle = MASK_CHAR.repeat(input.length() - prefixLength - suffixLength);
        
        return prefix + middle + suffix;
    }
    
    /**
     * Mask password completely
     */
    public String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        
        return MASK_CHAR.repeat(Math.min(password.length(), 8));
    }
    
    /**
     * Auto-detect and mask sensitive data
     */
    public String autoMask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Check for email
        if (EMAIL_PATTERN.matcher(input).matches()) {
            return maskEmail(input);
        }
        
        // Check for phone
        String digitsOnly = input.replaceAll("[^0-9]", "");
        if (PHONE_PATTERN.matcher(digitsOnly).matches()) {
            return maskPhone(input);
        }
        
        // Check for credit card
        if (CREDIT_CARD_PATTERN.matcher(digitsOnly).matches()) {
            return maskCreditCard(input);
        }
        
        // Check for SSN
        if (SSN_PATTERN.matcher(input).matches()) {
            return maskSSN(input);
        }
        
        // Default masking for potentially sensitive strings
        if (input.length() > 8) {
            return maskString(input, 2, 2);
        }
        
        return input;
    }
    
    /**
     * Mask data based on field name
     */
    public String maskByFieldName(String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        
        if (lowerFieldName.contains("email")) {
            return maskEmail(value);
        }
        
        if (lowerFieldName.contains("phone") || lowerFieldName.contains("mobile")) {
            return maskPhone(value);
        }
        
        if (lowerFieldName.contains("password") || lowerFieldName.contains("secret")) {
            return maskPassword(value);
        }
        
        if (lowerFieldName.contains("credit") || lowerFieldName.contains("card")) {
            return maskCreditCard(value);
        }
        
        if (lowerFieldName.contains("ssn") || lowerFieldName.contains("social")) {
            return maskSSN(value);
        }
        
        return autoMask(value);
    }
    
    private String maskDomain(String domain) {
        int dotIndex = domain.lastIndexOf('.');
        if (dotIndex <= 0) {
            return MASK_CHAR.repeat(domain.length());
        }
        
        String domainName = domain.substring(0, dotIndex);
        String tld = domain.substring(dotIndex);
        
        if (domainName.length() <= 2) {
            return MASK_CHAR.repeat(domainName.length()) + tld;
        }
        
        return maskString(domainName, 1, 1) + tld;
    }
}
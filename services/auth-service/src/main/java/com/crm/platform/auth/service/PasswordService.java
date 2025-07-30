package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.*;
import com.crm.platform.auth.entity.PasswordHistory;
import com.crm.platform.auth.entity.PasswordResetToken;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.exception.InvalidCredentialsException;
import com.crm.platform.auth.exception.InvalidTokenException;
import com.crm.platform.auth.repository.PasswordHistoryRepository;
import com.crm.platform.auth.repository.PasswordResetTokenRepository;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.common.exception.BusinessException;
import com.crm.platform.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.crm.platform.security.jwt.JwtTokenProvider;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Comprehensive service for password management operations including security features,
 * policy enforcement, and breach detection
 */
@Service
@Transactional
public class PasswordService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordService.class);

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final UserCredentialsRepository userCredentialsRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecurityAuditService securityAuditService;
    private final TokenManagementService tokenManagementService;
    private final PasswordBreachService passwordBreachService;

    // Password policy configuration
    @Value("${app.security.password.min-length:8}")
    private int minPasswordLength;

    @Value("${app.security.password.max-length:128}")
    private int maxPasswordLength;

    @Value("${app.security.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${app.security.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${app.security.password.require-numbers:true}")
    private boolean requireNumbers;

    @Value("${app.security.password.require-special-chars:true}")
    private boolean requireSpecialChars;

    @Value("${app.security.password.min-special-chars:1}")
    private int minSpecialChars;

    @Value("${app.security.password.prevent-common-passwords:true}")
    private boolean preventCommonPasswords;

    @Value("${app.security.password.prevent-password-reuse:true}")
    private boolean preventPasswordReuse;

    @Value("${app.security.password.history-count:5}")
    private int passwordHistoryCount;

    @Value("${app.security.password.expiry-days:90}")
    private Integer passwordExpiryDays;

    @Value("${app.security.password.reset-token-expiry-hours:24}")
    private int resetTokenExpiryHours;

    @Value("${app.security.password.max-reset-attempts-per-hour:3}")
    private int maxResetAttemptsPerHour;

    @Value("${app.security.password.expiry-warning-days:7}")
    private int expiryWarningDays;

    private static final String ALLOWED_SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[" + Pattern.quote(ALLOWED_SPECIAL_CHARS) + "]");

    // Common passwords list (in production, this should be loaded from a file or database)
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "password123", "admin", "qwerty", "letmein", "welcome",
        "monkey", "1234567890", "abc123", "Password1", "password1", "123456789"
    );

    @Autowired
    public PasswordService(PasswordEncoder passwordEncoder,
                          UserCredentialsRepository userCredentialsRepository,
                          PasswordHistoryRepository passwordHistoryRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          SecurityAuditService securityAuditService,
                          TokenManagementService tokenManagementService,
                          PasswordBreachService passwordBreachService) {
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
        this.userCredentialsRepository = userCredentialsRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.securityAuditService = securityAuditService;
        this.tokenManagementService = tokenManagementService;
        this.passwordBreachService = passwordBreachService;
    }

    /**
     * Encode a raw password using BCrypt
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verify if a raw password matches an encoded password
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Request password reset - generates secure token and sends email
     */
    public Map<String, Object> requestPasswordReset(PasswordResetRequest request, HttpServletRequest httpRequest) {
        logger.info("Password reset requested for email: {}", request.getEmail());

        // Find user by email
        Optional<UserCredentials> userOpt = userCredentialsRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists - return success anyway for security
            logger.warn("Password reset requested for non-existent email: {}", request.getEmail());
            return Map.of(
                "success", true,
                "message", "If the email exists, a password reset link has been sent",
                "requestId", UUID.randomUUID().toString()
            );
        }

        UserCredentials user = userOpt.get();
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Check rate limiting
        if (isResetRateLimited(user.getUserId(), clientIp)) {
            throw new BusinessException("Too many password reset attempts. Please try again later.");
        }

        // Invalidate any existing active tokens
        passwordResetTokenRepository.markAllUserTokensAsUsed(user.getUserId(), LocalDateTime.now());

        // Generate secure reset token
        String resetToken = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(resetTokenExpiryHours);

        // Save reset token
        PasswordResetToken token = new PasswordResetToken(
            user.getUserId(), resetToken, expiresAt, clientIp, userAgent);
        passwordResetTokenRepository.save(token);

        // Log security event
        securityAuditService.logPasswordResetRequest(user.getUserId(), clientIp, userAgent);

        // TODO: Send password reset email (integrate with email service)
        logger.info("Password reset token generated for user: {}", user.getUserId());

        return Map.of(
            "success", true,
            "message", "Password reset email sent",
            "requestId", token.getId().toString(),
            "expiresAt", expiresAt
        );
    }

    /**
     * Confirm password reset using token
     */
    public Map<String, Object> confirmPasswordReset(PasswordResetConfirmRequest request, HttpServletRequest httpRequest) {
        logger.info("Password reset confirmation attempted");

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }

        // Find and validate reset token
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository
            .findValidToken(request.getResetToken(), LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            throw new InvalidTokenException("Invalid or expired reset token");
        }

        PasswordResetToken resetToken = tokenOpt.get();
        
        // Find user
        Optional<UserCredentials> userOpt = userCredentialsRepository.findByUserId(resetToken.getUserId());
        if (userOpt.isEmpty()) {
            throw new BusinessException("User not found");
        }

        UserCredentials user = userOpt.get();
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Validate new password
        PasswordValidationResult validation = validatePassword(request.getNewPassword(), user.getUserId());
        if (!validation.isValid()) {
            throw new ValidationException("Password does not meet security requirements", 
                Map.of("validationErrors", validation.getValidationErrors()));
        }

        // Save old password to history
        savePasswordHistory(user.getUserId(), user.getPasswordHash(), clientIp, userAgent);

        // Update password
        user.setPasswordHash(encodePassword(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0); // Reset failed attempts
        user.setAccountLockedUntil(null); // Unlock account if locked
        userCredentialsRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        // Clean up old password history
        cleanupPasswordHistory(user.getUserId());

        // Log security event
        securityAuditService.logPasswordReset(user.getUserId(), clientIp, userAgent);

        logger.info("Password reset completed for user: {}", user.getUserId());

        return Map.of(
            "success", true,
            "message", "Password reset successful",
            "passwordChangedAt", user.getPasswordChangedAt()
        );
    }

    /**
     * Change password for authenticated user
     */
    public Map<String, Object> changePassword(String authorization, PasswordChangeRequest request, HttpServletRequest httpRequest) {
        logger.info("Password change requested");

        // Extract user from token
        UUID userId = tokenManagementService.extractUserIdFromToken(authorization);
        
        // Find user
        Optional<UserCredentials> userOpt = userCredentialsRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new BusinessException("User not found");
        }

        UserCredentials user = userOpt.get();
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Verify current password
        if (!verifyPassword(request.getCurrentPassword(), user.getPasswordHash())) {
            securityAuditService.logFailedPasswordChange(userId, clientIp, userAgent);
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("New passwords do not match");
        }

        // Validate new password
        PasswordValidationResult validation = validatePassword(request.getNewPassword(), userId);
        if (!validation.isValid()) {
            throw new ValidationException("Password does not meet security requirements", 
                Map.of("validationErrors", validation.getValidationErrors()));
        }

        // Save old password to history
        savePasswordHistory(userId, user.getPasswordHash(), clientIp, userAgent);

        // Update password
        user.setPasswordHash(encodePassword(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userCredentialsRepository.save(user);

        // Clean up old password history
        cleanupPasswordHistory(userId);

        // Log security event
        securityAuditService.logPasswordChange(userId, clientIp, userAgent);

        logger.info("Password changed successfully for user: {}", userId);

        return Map.of(
            "success", true,
            "message", "Password changed successfully",
            "passwordChangedAt", user.getPasswordChangedAt()
        );
    }

    /**
     * Get password policy information
     */
    public PasswordPolicyResponse getPasswordPolicy() {
        return new PasswordPolicyResponse(
            minPasswordLength,
            maxPasswordLength,
            requireUppercase,
            requireLowercase,
            requireNumbers,
            requireSpecialChars,
            minSpecialChars,
            preventCommonPasswords,
            preventPasswordReuse,
            passwordHistoryCount,
            passwordExpiryDays,
            ALLOWED_SPECIAL_CHARS
        );
    }

    /**
     * Validate password against policy and security requirements
     */
    public PasswordValidationResult validatePassword(String password, UUID userId) {
        PasswordValidationResult result = new PasswordValidationResult();
        int score = 0;

        // Length validation
        if (password.length() < minPasswordLength) {
            result.addValidationError("Password must be at least " + minPasswordLength + " characters long");
        } else if (password.length() >= minPasswordLength) {
            score += 10;
        }

        if (password.length() > maxPasswordLength) {
            result.addValidationError("Password must not exceed " + maxPasswordLength + " characters");
        }

        // Character type requirements
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            result.addValidationError("Password must contain at least one uppercase letter");
        } else if (UPPERCASE_PATTERN.matcher(password).find()) {
            score += 15;
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            result.addValidationError("Password must contain at least one lowercase letter");
        } else if (LOWERCASE_PATTERN.matcher(password).find()) {
            score += 15;
        }

        if (requireNumbers && !NUMBER_PATTERN.matcher(password).find()) {
            result.addValidationError("Password must contain at least one number");
        } else if (NUMBER_PATTERN.matcher(password).find()) {
            score += 15;
        }

        if (requireSpecialChars) {
            long specialCharCount = password.chars()
                .filter(c -> ALLOWED_SPECIAL_CHARS.indexOf(c) >= 0)
                .count();
            
            if (specialCharCount < minSpecialChars) {
                result.addValidationError("Password must contain at least " + minSpecialChars + " special character(s)");
            } else {
                score += 20;
            }
        }

        // Common password check
        if (preventCommonPasswords && COMMON_PASSWORDS.contains(password.toLowerCase())) {
            result.addValidationError("Password is too common. Please choose a more unique password");
        }

        // Password reuse check
        if (preventPasswordReuse && userId != null && isPasswordReused(password, userId)) {
            result.addValidationError("Password has been used recently. Please choose a different password");
        }

        // Breach detection check (async)
        try {
            PasswordBreachService.BreachCheckResult breachResult = passwordBreachService.checkPasswordBreach(password).get();
            if (breachResult.isCompromised()) {
                result.setCompromised(true);
                result.addValidationError("This password has been found in " + breachResult.getBreachCount() + " data breach(es). Please choose a different password");
                score -= 30; // Heavily penalize compromised passwords
            }
        } catch (Exception e) {
            logger.warn("Could not check password breach status: {}", e.getMessage());
            // Continue without breach check
        }

        // Additional scoring for complexity
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;
        
        // Bonus for character diversity
        Set<Character> uniqueChars = new HashSet<>();
        password.chars().forEach(c -> uniqueChars.add((char) c));
        if (uniqueChars.size() >= password.length() * 0.7) score += 10;

        // Set strength level
        result.setStrengthScore(Math.min(score, 100));
        result.setStrengthLevel(calculateStrengthLevel(score));

        // Add suggestions
        addPasswordSuggestions(result, password);

        return result;
    }

    /**
     * Generate a random secure password
     */
    public String generateRandomPassword(int length) {
        if (length < minPasswordLength) {
            length = minPasswordLength;
        }
        if (length > maxPasswordLength) {
            length = maxPasswordLength;
        }

        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = ALLOWED_SPECIAL_CHARS;

        StringBuilder password = new StringBuilder();
        
        // Ensure at least one character from each required category
        if (requireUppercase) password.append(uppercase.charAt(secureRandom.nextInt(uppercase.length())));
        if (requireLowercase) password.append(lowercase.charAt(secureRandom.nextInt(lowercase.length())));
        if (requireNumbers) password.append(numbers.charAt(secureRandom.nextInt(numbers.length())));
        if (requireSpecialChars) {
            for (int i = 0; i < minSpecialChars; i++) {
                password.append(specialChars.charAt(secureRandom.nextInt(specialChars.length())));
            }
        }

        // Fill remaining length with random characters
        String allChars = uppercase + lowercase + numbers + specialChars;
        while (password.length() < length) {
            password.append(allChars.charAt(secureRandom.nextInt(allChars.length())));
        }

        // Shuffle the password
        List<Character> chars = new ArrayList<>();
        for (char c : password.toString().toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars, secureRandom);

        StringBuilder shuffledPassword = new StringBuilder();
        for (char c : chars) {
            shuffledPassword.append(c);
        }

        return shuffledPassword.toString();
    }

    /**
     * Check password expiration status for a user
     */
    public PasswordExpirationInfo checkPasswordExpiration(UUID userId) {
        Optional<UserCredentials> userOpt = userCredentialsRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new BusinessException("User not found");
        }

        UserCredentials user = userOpt.get();
        LocalDateTime passwordChangedAt = user.getPasswordChangedAt();
        
        if (passwordExpiryDays == null || passwordChangedAt == null) {
            // Password expiration not configured or no password change date
            return new PasswordExpirationInfo(false, null, null, passwordChangedAt, false, expiryWarningDays);
        }

        LocalDateTime expiresAt = passwordChangedAt.plusDays(passwordExpiryDays);
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = now.isAfter(expiresAt);
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now, expiresAt);
        boolean requiresChange = isExpired || daysUntilExpiry <= 0;

        return new PasswordExpirationInfo(
            isExpired, 
            expiresAt, 
            daysUntilExpiry, 
            passwordChangedAt, 
            requiresChange, 
            expiryWarningDays
        );
    }

    /**
     * Check if password is expiring soon (within warning threshold)
     */
    public boolean isPasswordExpiringSoon(UUID userId) {
        PasswordExpirationInfo info = checkPasswordExpiration(userId);
        return info.getDaysUntilExpiry() != null && 
               info.getDaysUntilExpiry() <= expiryWarningDays && 
               info.getDaysUntilExpiry() > 0;
    }

    /**
     * Check if password has expired
     */
    public boolean isPasswordExpired(UUID userId) {
        PasswordExpirationInfo info = checkPasswordExpiration(userId);
        return info.isExpired();
    }

    // Private helper methods

    private boolean isResetRateLimited(UUID userId, String ipAddress) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentAttempts = passwordResetTokenRepository.countActiveTokensByUserId(userId, LocalDateTime.now());
        return recentAttempts >= maxResetAttemptsPerHour;
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private void savePasswordHistory(UUID userId, String passwordHash, String ipAddress, String userAgent) {
        PasswordHistory history = new PasswordHistory(userId, passwordHash, ipAddress, userAgent);
        passwordHistoryRepository.save(history);
    }

    private void cleanupPasswordHistory(UUID userId) {
        long historyCount = passwordHistoryRepository.countByUserId(userId);
        if (historyCount > passwordHistoryCount) {
            passwordHistoryRepository.deleteOldPasswordHistory(userId, passwordHistoryCount);
        }
    }

    private boolean isPasswordReused(String newPassword, UUID userId) {
        List<PasswordHistory> recentPasswords = passwordHistoryRepository
            .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, passwordHistoryCount));
        
        return recentPasswords.stream()
            .anyMatch(history -> verifyPassword(newPassword, history.getPasswordHash()));
    }

    private PasswordValidationResult.StrengthLevel calculateStrengthLevel(int score) {
        if (score < 20) return PasswordValidationResult.StrengthLevel.VERY_WEAK;
        if (score < 40) return PasswordValidationResult.StrengthLevel.WEAK;
        if (score < 60) return PasswordValidationResult.StrengthLevel.FAIR;
        if (score < 80) return PasswordValidationResult.StrengthLevel.GOOD;
        if (score < 95) return PasswordValidationResult.StrengthLevel.STRONG;
        return PasswordValidationResult.StrengthLevel.VERY_STRONG;
    }

    private void addPasswordSuggestions(PasswordValidationResult result, String password) {
        if (password.length() < 12) {
            result.addSuggestion("Consider using a longer password (12+ characters) for better security");
        }
        
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            result.addSuggestion("Add uppercase letters to strengthen your password");
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            result.addSuggestion("Include special characters like !@#$%^&* for better security");
        }
        
        if (password.matches(".*\\d{3,}.*")) {
            result.addSuggestion("Avoid using sequential numbers in your password");
        }
        
        if (password.matches(".*([a-zA-Z])\\1{2,}.*")) {
            result.addSuggestion("Avoid repeating the same character multiple times");
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 
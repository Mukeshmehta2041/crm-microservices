package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.*;
import com.crm.platform.auth.entity.EmailVerificationToken;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.exception.InvalidTokenException;
import com.crm.platform.auth.repository.EmailVerificationTokenRepository;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.common.exception.BusinessException;
import com.crm.platform.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive service for email verification operations including token generation,
 * validation, rate limiting, and audit logging
 */
@Service
@Transactional
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final SecurityAuditService securityAuditService;
    private final RateLimitingService rateLimitingService;
    private final EmailService emailService;
    private final SecureRandom secureRandom;

    // Configuration properties
    @Value("${app.security.email-verification.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.security.email-verification.max-attempts:3}")
    private int maxVerificationAttempts;

    @Value("${app.security.email-verification.max-resends-per-hour:3}")
    private int maxResendsPerHour;

    @Value("${app.security.email-verification.resend-cooldown-minutes:5}")
    private int resendCooldownMinutes;

    @Value("${app.security.email-verification.cleanup-days:7}")
    private int cleanupDays;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String verificationBaseUrl;

    @Autowired
    public EmailVerificationService(EmailVerificationTokenRepository emailVerificationTokenRepository,
                                  UserCredentialsRepository userCredentialsRepository,
                                  SecurityAuditService securityAuditService,
                                  RateLimitingService rateLimitingService,
                                  EmailService emailService) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.securityAuditService = securityAuditService;
        this.rateLimitingService = rateLimitingService;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate and send email verification token
     */
    public EmailVerificationResponse generateVerificationToken(UUID userId, String email, 
                                                             EmailVerificationToken.VerificationType verificationType,
                                                             HttpServletRequest httpRequest) {
        logger.info("Generating email verification token for user: {} email: {}", userId, email);

        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Check rate limiting
        if (isResendRateLimited(email, clientIp)) {
            throw new BusinessException("Too many verification emails sent. Please try again later.");
        }

        // Invalidate any existing active tokens for this email and verification type
        invalidateActiveTokens(userId, email, verificationType);

        // Generate secure verification token
        String verificationToken = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpiryHours);

        // Create and save verification token
        EmailVerificationToken token = new EmailVerificationToken(
            userId, email, verificationToken, expiresAt, verificationType, clientIp, userAgent);
        token.setMaxAttempts(maxVerificationAttempts);
        emailVerificationTokenRepository.save(token);

        // Log security event
        securityAuditService.logEmailVerificationTokenGenerated(userId, email, verificationType.name(), clientIp, userAgent);

        // Send verification email
        sendVerificationEmail(email, verificationToken, verificationType);
        logger.info("Email verification token generated: {} for user: {}", token.getId(), userId);

        return new EmailVerificationResponse(
            token.getId(),
            email,
            false,
            null,
            expiresAt,
            verificationType.name(),
            maxVerificationAttempts,
            "Verification email sent successfully"
        );
    }

    /**
     * Verify email using verification token
     */
    public EmailVerificationResponse verifyEmail(EmailVerificationRequest request, HttpServletRequest httpRequest) {
        logger.info("Email verification attempted with token");

        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Find and validate verification token
        Optional<EmailVerificationToken> tokenOpt = emailVerificationTokenRepository
            .findValidToken(request.getVerificationToken(), LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            // Check if token exists but is invalid
            Optional<EmailVerificationToken> existingToken = emailVerificationTokenRepository
                .findByToken(request.getVerificationToken());
            
            if (existingToken.isPresent()) {
                EmailVerificationToken token = existingToken.get();
                token.incrementAttempts();
                emailVerificationTokenRepository.save(token);
                
                securityAuditService.logEmailVerificationFailed(
                    token.getUserId(), token.getEmail(), "Invalid or expired token", clientIp, userAgent);
                
                if (token.isExpired()) {
                    throw new InvalidTokenException("Verification token has expired");
                } else if (!token.hasAttemptsRemaining()) {
                    throw new InvalidTokenException("Maximum verification attempts exceeded");
                } else {
                    throw new InvalidTokenException("Token has already been used");
                }
            } else {
                securityAuditService.logEmailVerificationFailed(
                    null, null, "Token not found", clientIp, userAgent);
                throw new InvalidTokenException("Invalid verification token");
            }
        }

        EmailVerificationToken verificationToken = tokenOpt.get();
        
        // Find user credentials
        Optional<UserCredentials> userOpt = userCredentialsRepository.findByUserId(verificationToken.getUserId());
        if (userOpt.isEmpty()) {
            throw new BusinessException("User not found");
        }

        UserCredentials user = userOpt.get();

        // Mark token as verified
        verificationToken.markAsVerified();
        emailVerificationTokenRepository.save(verificationToken);

        // Update user email verification status
        if (verificationToken.getVerificationType() == EmailVerificationToken.VerificationType.REGISTRATION) {
            user.setEmailVerified(true);
            userCredentialsRepository.save(user);
        } else if (verificationToken.getVerificationType() == EmailVerificationToken.VerificationType.EMAIL_CHANGE) {
            // Update user's email address
            user.setEmail(verificationToken.getEmail());
            user.setEmailVerified(true);
            userCredentialsRepository.save(user);
        }

        // Invalidate all other active tokens for this user and email
        emailVerificationTokenRepository.markAllEmailTokensAsVerified(verificationToken.getEmail(), LocalDateTime.now());

        // Log successful verification
        securityAuditService.logEmailVerificationSuccess(
            user.getUserId(), verificationToken.getEmail(), verificationToken.getVerificationType().name(), clientIp, userAgent);

        logger.info("Email verification completed for user: {} email: {}", user.getUserId(), verificationToken.getEmail());

        return new EmailVerificationResponse(
            verificationToken.getId(),
            verificationToken.getEmail(),
            true,
            verificationToken.getVerifiedAt(),
            verificationToken.getExpiresAt(),
            verificationToken.getVerificationType().name(),
            0,
            "Email verified successfully"
        );
    }

    /**
     * Resend verification email
     */
    public EmailVerificationResponse resendVerificationEmail(ResendVerificationRequest request, HttpServletRequest httpRequest) {
        logger.info("Resend verification email requested for: {}", request.getEmail());

        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Find user by email
        Optional<UserCredentials> userOpt = userCredentialsRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists - return success anyway for security
            logger.warn("Resend verification requested for non-existent email: {}", request.getEmail());
            return new EmailVerificationResponse(
                UUID.randomUUID(),
                request.getEmail(),
                false,
                null,
                LocalDateTime.now().plusHours(tokenExpiryHours),
                EmailVerificationToken.VerificationType.REGISTRATION.name(),
                maxVerificationAttempts,
                "If the email exists and is unverified, a verification email has been sent"
            );
        }

        UserCredentials user = userOpt.get();

        // Check if email is already verified
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException("Email is already verified");
        }

        // Check rate limiting
        if (isResendRateLimited(request.getEmail(), clientIp)) {
            throw new BusinessException("Too many verification emails sent. Please try again later.");
        }

        // Generate new verification token
        return generateVerificationToken(
            user.getUserId(), 
            request.getEmail(), 
            EmailVerificationToken.VerificationType.REGISTRATION,
            httpRequest
        );
    }

    /**
     * Get email verification status
     */
    public EmailVerificationStatusResponse getVerificationStatus(String email) {
        logger.debug("Getting verification status for email: {}", email);

        // Find user by email
        Optional<UserCredentials> userOpt = userCredentialsRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new BusinessException("Email not found");
        }

        UserCredentials user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // Get pending verifications
        List<EmailVerificationToken> activeTokens = emailVerificationTokenRepository
            .findActiveTokensByEmail(email, now);

        List<EmailVerificationStatusResponse.PendingVerification> pendingVerifications = activeTokens.stream()
            .map(token -> new EmailVerificationStatusResponse.PendingVerification(
                token.getVerificationType().name(),
                token.getExpiresAt(),
                token.getMaxAttempts() - token.getAttempts()
            ))
            .collect(Collectors.toList());

        // Check if can resend
        boolean canResend = !isResendRateLimited(email, null);
        LocalDateTime nextResendAt = null;
        if (!canResend) {
            // Calculate next resend time based on most recent token
            Optional<EmailVerificationToken> mostRecent = activeTokens.stream()
                .max(Comparator.comparing(EmailVerificationToken::getCreatedAt));
            if (mostRecent.isPresent()) {
                nextResendAt = mostRecent.get().getCreatedAt().plusMinutes(resendCooldownMinutes);
            }
        }

        return new EmailVerificationStatusResponse(
            email,
            Boolean.TRUE.equals(user.getEmailVerified()),
            null, // We don't track when email was verified in UserCredentials
            pendingVerifications,
            canResend,
            nextResendAt,
            !Boolean.TRUE.equals(user.getEmailVerified())
        );
    }

    /**
     * Request email change verification
     */
    public EmailVerificationResponse requestEmailChange(UUID userId, EmailChangeRequest request, HttpServletRequest httpRequest) {
        logger.info("Email change verification requested for user: {}", userId);

        // Find user
        Optional<UserCredentials> userOpt = userCredentialsRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new BusinessException("User not found");
        }

        UserCredentials user = userOpt.get();

        // Verify current password using Spring Security's PasswordEncoder directly
        org.springframework.security.crypto.password.PasswordEncoder passwordEncoder = 
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        // Check if new email is already in use
        Optional<UserCredentials> existingUser = userCredentialsRepository.findByEmail(request.getNewEmail());
        if (existingUser.isPresent() && !existingUser.get().getUserId().equals(userId)) {
            throw new ValidationException("Email address is already in use");
        }

        // Generate verification token for email change
        return generateVerificationToken(
            userId,
            request.getNewEmail(),
            EmailVerificationToken.VerificationType.EMAIL_CHANGE,
            httpRequest
        );
    }

    /**
     * Clean up expired and old verification tokens
     */
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            logger.debug("Starting cleanup of expired email verification tokens");
            
            LocalDateTime now = LocalDateTime.now();
            emailVerificationTokenRepository.deleteExpiredTokens(now);
            
            LocalDateTime cutoffDate = now.minusDays(cleanupDays);
            emailVerificationTokenRepository.deleteTokensOlderThan(cutoffDate);
            
            logger.debug("Completed cleanup of expired email verification tokens");
        } catch (Exception e) {
            logger.error("Error during email verification token cleanup", e);
        }
    }

    // Private helper methods

    private boolean isResendRateLimited(String email, String ipAddress) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentAttempts = emailVerificationTokenRepository.countActiveTokensByEmailSince(
            email, LocalDateTime.now(), oneHourAgo);
        return recentAttempts >= maxResendsPerHour;
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private void invalidateActiveTokens(UUID userId, String email, EmailVerificationToken.VerificationType verificationType) {
        LocalDateTime now = LocalDateTime.now();
        List<EmailVerificationToken> activeTokens = emailVerificationTokenRepository
            .findActiveTokensByEmail(email, now);
        
        for (EmailVerificationToken token : activeTokens) {
            if (token.getVerificationType() == verificationType) {
                token.setIsVerified(true);
                token.setVerifiedAt(now);
                emailVerificationTokenRepository.save(token);
            }
        }
    }

    private void sendVerificationEmail(String email, String token, EmailVerificationToken.VerificationType type) {
        try {
            String subject;
            String templateName;
            
            switch (type) {
                case REGISTRATION:
                    subject = "Verify your email address";
                    templateName = "email-verification";
                    break;
                case EMAIL_CHANGE:
                    subject = "Confirm your new email address";
                    templateName = "email-change-verification";
                    break;
                case PASSWORD_RESET_VERIFICATION:
                    subject = "Verify password reset request";
                    templateName = "password-reset-verification";
                    break;
                default:
                    subject = "Email verification required";
                    templateName = "email-verification";
            }
            
            // Create verification URL
            String verificationUrl = buildVerificationUrl(token);
            
            // Prepare email context
            Map<String, Object> emailContext = Map.of(
                "email", email,
                "verificationUrl", verificationUrl,
                "token", token,
                "expiryHours", tokenExpiryHours,
                "verificationType", type.name()
            );
            
            // Send email using email service
            emailService.sendTemplatedEmail(email, subject, templateName, emailContext);
            
            logger.debug("Verification email sent to: {} with template: {}", email, templateName);
            
        } catch (Exception e) {
            logger.error("Failed to send verification email to: " + email, e);
            // Don't throw exception to avoid breaking the verification token generation
            // The token is still valid and can be used if the user gets the email through other means
        }
    }

    private String buildVerificationUrl(String token) {
        // Build verification URL based on configuration
        String baseUrl = verificationBaseUrl != null ? verificationBaseUrl : "http://localhost:3000";
        return baseUrl + "/verify-email?token=" + token;
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
package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * Service for handling CAPTCHA generation and verification
 */
@Service
public class CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;
    private final SecurityAuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${captcha.enabled:true}")
    private boolean captchaEnabled;

    @Value("${captcha.recaptcha.enabled:false}")
    private boolean recaptchaEnabled;

    @Value("${captcha.recaptcha.secret-key:}")
    private String recaptchaSecretKey;

    @Value("${captcha.recaptcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String recaptchaVerifyUrl;

    @Value("${captcha.simple.length:5}")
    private int simpleCaptchaLength;

    @Value("${captcha.simple.expiry-minutes:10}")
    private int simpleCaptchaExpiryMinutes;

    @Value("${captcha.simple.width:200}")
    private int captchaWidth;

    @Value("${captcha.simple.height:60}")
    private int captchaHeight;

    @Autowired
    public CaptchaService(RedisTemplate<String, Object> redisTemplate, 
                         RestTemplate restTemplate, 
                         SecurityAuditService auditService) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.auditService = auditService;
    }

    /**
     * Generate a simple CAPTCHA challenge
     */
    public SimpleCaptchaChallenge generateSimpleCaptcha(String sessionId) {
        if (!captchaEnabled) {
            return new SimpleCaptchaChallenge("", "data:image/png;base64,", false);
        }

        try {
            // Generate random text
            String captchaText = generateRandomText(simpleCaptchaLength);
            
            // Create image
            String imageBase64 = createCaptchaImage(captchaText);
            
            // Store in Redis with expiry
            String key = "captcha:" + sessionId;
            redisTemplate.opsForValue().set(key, captchaText.toLowerCase(), 
                                          simpleCaptchaExpiryMinutes, TimeUnit.MINUTES);
            
            logger.debug("Generated simple CAPTCHA for session: {}", sessionId);
            
            return new SimpleCaptchaChallenge(sessionId, imageBase64, true);
            
        } catch (Exception e) {
            logger.error("Error generating simple CAPTCHA", e);
            return new SimpleCaptchaChallenge("", "", false);
        }
    }

    /**
     * Verify simple CAPTCHA response
     */
    public boolean verifySimpleCaptcha(String sessionId, String userResponse, String ipAddress) {
        if (!captchaEnabled) {
            return true;
        }

        try {
            String key = "captcha:" + sessionId;
            String storedCaptcha = (String) redisTemplate.opsForValue().get(key);
            
            if (storedCaptcha == null) {
                logger.warn("CAPTCHA verification failed - expired or invalid session: {}", sessionId);
                auditService.logSecurityEvent(null, null, "CAPTCHA_VERIFICATION_FAILED",
                    "CAPTCHA verification failed - expired or invalid session: " + sessionId,
                    com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.FAILURE,
                    ipAddress, null, null);
                return false;
            }

            boolean isValid = storedCaptcha.equalsIgnoreCase(userResponse.trim());
            
            if (isValid) {
                // Remove used CAPTCHA
                redisTemplate.delete(key);
                logger.debug("CAPTCHA verification successful for session: {}", sessionId);
                auditService.logSecurityEvent(null, null, "CAPTCHA_VERIFICATION_SUCCESS",
                    "CAPTCHA verification successful for session: " + sessionId,
                    com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.SUCCESS,
                    ipAddress, null, null);
            } else {
                logger.warn("CAPTCHA verification failed - incorrect response for session: {}", sessionId);
                auditService.logSecurityEvent(null, null, "CAPTCHA_VERIFICATION_FAILED",
                    "CAPTCHA verification failed - incorrect response for session: " + sessionId,
                    com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.FAILURE,
                    ipAddress, null, null);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error verifying simple CAPTCHA", e);
            return false;
        }
    }

    /**
     * Verify Google reCAPTCHA response
     */
    public boolean verifyRecaptcha(String recaptchaResponse, String ipAddress) {
        if (!captchaEnabled || !recaptchaEnabled || recaptchaSecretKey.isEmpty()) {
            return true;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", recaptchaSecretKey);
            params.add("response", recaptchaResponse);
            params.add("remoteip", ipAddress);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(recaptchaVerifyUrl, request, Map.class);
            
            if (response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                
                if (Boolean.TRUE.equals(success)) {
                    logger.debug("reCAPTCHA verification successful for IP: {}", ipAddress);
                    auditService.logSecurityEvent(null, null, "RECAPTCHA_VERIFICATION_SUCCESS",
                        "reCAPTCHA verification successful",
                        com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.SUCCESS,
                        ipAddress, null, null);
                    return true;
                } else {
                    logger.warn("reCAPTCHA verification failed for IP: {}", ipAddress);
                    auditService.logSecurityEvent(null, null, "RECAPTCHA_VERIFICATION_FAILED",
                        "reCAPTCHA verification failed",
                        com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.FAILURE,
                        ipAddress, null, null);
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error verifying reCAPTCHA", e);
            return false;
        }
    }

    /**
     * Check if CAPTCHA is required based on rate limiting
     */
    public boolean isCaptchaRequired(String identifier, String operation) {
        if (!captchaEnabled) {
            return false;
        }

        String key = "captcha_required:" + identifier + ":" + operation;
        return redisTemplate.hasKey(key);
    }

    /**
     * Generate random text for CAPTCHA
     */
    private String generateRandomText(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        
        return result.toString();
    }

    /**
     * Create CAPTCHA image
     */
    private String createCaptchaImage(String text) throws IOException {
        BufferedImage image = new BufferedImage(captchaWidth, captchaHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Fill background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, captchaWidth, captchaHeight);
        
        // Add noise lines
        g2d.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 10; i++) {
            int x1 = secureRandom.nextInt(captchaWidth);
            int y1 = secureRandom.nextInt(captchaHeight);
            int x2 = secureRandom.nextInt(captchaWidth);
            int y2 = secureRandom.nextInt(captchaHeight);
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Draw text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int x = (captchaWidth - textWidth) / 2;
        int y = (captchaHeight + textHeight) / 2 - fm.getDescent();
        
        // Add slight rotation and distortion to each character
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int charX = x + (textWidth / text.length()) * i;
            int charY = y + secureRandom.nextInt(10) - 5; // Random vertical offset
            
            Graphics2D charG2d = (Graphics2D) g2d.create();
            charG2d.rotate(Math.toRadians(secureRandom.nextInt(30) - 15), charX, charY); // Random rotation
            charG2d.drawString(String.valueOf(c), charX, charY);
            charG2d.dispose();
        }
        
        // Add noise dots
        g2d.setColor(Color.GRAY);
        for (int i = 0; i < 50; i++) {
            int x1 = secureRandom.nextInt(captchaWidth);
            int y1 = secureRandom.nextInt(captchaHeight);
            g2d.fillOval(x1, y1, 2, 2);
        }
        
        g2d.dispose();
        
        // Convert to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Clean up expired CAPTCHA challenges
     */
    public void cleanupExpiredCaptchas() {
        // Redis TTL handles this automatically, but we can add manual cleanup if needed
        logger.debug("CAPTCHA cleanup completed (handled by Redis TTL)");
    }

    /**
     * Get CAPTCHA configuration
     */
    public Map<String, Object> getCaptchaConfiguration() {
        return Map.of(
            "enabled", captchaEnabled,
            "recaptcha_enabled", recaptchaEnabled,
            "simple_captcha_length", simpleCaptchaLength,
            "simple_captcha_expiry_minutes", simpleCaptchaExpiryMinutes,
            "image_width", captchaWidth,
            "image_height", captchaHeight
        );
    }

    /**
     * Simple CAPTCHA challenge holder
     */
    public static class SimpleCaptchaChallenge {
        private final String sessionId;
        private final String imageBase64;
        private final boolean success;

        public SimpleCaptchaChallenge(String sessionId, String imageBase64, boolean success) {
            this.sessionId = sessionId;
            this.imageBase64 = imageBase64;
            this.success = success;
        }

        public String getSessionId() { return sessionId; }
        public String getImageBase64() { return imageBase64; }
        public boolean isSuccess() { return success; }
    }
}
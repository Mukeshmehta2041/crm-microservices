package com.crm.platform.auth.service.impl;

import com.crm.platform.auth.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Simple implementation of EmailService
 * In production, this would integrate with actual email providers like SendGrid, AWS SES, etc.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:noreply@crm-platform.com}")
    private String fromEmail;

    @Override
    public void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> context) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send templated email to: {} with template: {}", to, templateName);
            logger.debug("Email context: {}", context);
            return;
        }

        try {
            // In production, this would:
            // 1. Load the email template
            // 2. Process template with context variables
            // 3. Send via email provider API
            
            String processedContent = processTemplate(templateName, context);
            sendActualEmail(to, subject, processedContent);
            
            logger.info("Templated email sent successfully to: {} with template: {}", to, templateName);
            
        } catch (Exception e) {
            logger.error("Failed to send templated email to: " + to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendTextEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send text email to: {}", to);
            logger.debug("Email body: {}", body);
            return;
        }

        try {
            sendActualEmail(to, subject, body);
            logger.info("Text email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send text email to: " + to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send HTML email to: {}", to);
            logger.debug("Email HTML body: {}", htmlBody);
            return;
        }

        try {
            sendActualEmail(to, subject, htmlBody);
            logger.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send HTML email to: " + to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String processTemplate(String templateName, Map<String, Object> context) {
        // Simple template processing - in production use a proper template engine
        String template = getTemplate(templateName);
        
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            template = template.replace(placeholder, value);
        }
        
        return template;
    }

    private String getTemplate(String templateName) {
        // In production, load from resources or database
        switch (templateName) {
            case "email-verification":
                return """
                    <html>
                    <body>
                        <h2>Verify Your Email Address</h2>
                        <p>Hello,</p>
                        <p>Please click the link below to verify your email address:</p>
                        <p><a href="{{verificationUrl}}">Verify Email</a></p>
                        <p>This link will expire in {{expiryHours}} hours.</p>
                        <p>If you didn't create an account, please ignore this email.</p>
                    </body>
                    </html>
                    """;
            case "email-change-verification":
                return """
                    <html>
                    <body>
                        <h2>Confirm Your New Email Address</h2>
                        <p>Hello,</p>
                        <p>Please click the link below to confirm your new email address:</p>
                        <p><a href="{{verificationUrl}}">Confirm Email Change</a></p>
                        <p>This link will expire in {{expiryHours}} hours.</p>
                        <p>If you didn't request this change, please contact support.</p>
                    </body>
                    </html>
                    """;
            case "password-reset":
                return """
                    <html>
                    <body>
                        <h2>Password Reset Request</h2>
                        <p>Hello,</p>
                        <p>You requested a password reset. Click the link below to reset your password:</p>
                        <p><a href="{{resetUrl}}">Reset Password</a></p>
                        <p>This link will expire in {{expiryHours}} hours.</p>
                        <p>If you didn't request this reset, please ignore this email.</p>
                    </body>
                    </html>
                    """;
            default:
                return "<html><body><p>{{message}}</p></body></html>";
        }
    }

    private void sendActualEmail(String to, String subject, String content) {
        // In production, integrate with actual email service
        // For now, just log the email details
        logger.info("Sending email:");
        logger.info("To: {}", to);
        logger.info("From: {}", fromEmail);
        logger.info("Subject: {}", subject);
        logger.debug("Content: {}", content);
        
        // Simulate email sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
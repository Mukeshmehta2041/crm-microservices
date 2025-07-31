package com.crm.platform.auth.service;

import java.util.Map;

/**
 * Service interface for sending emails
 */
public interface EmailService {
    
    /**
     * Send a templated email
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param templateName Template name
     * @param context Template context variables
     */
    void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> context);
    
    /**
     * Send a simple text email
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body
     */
    void sendTextEmail(String to, String subject, String body);
    
    /**
     * Send an HTML email
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlBody HTML email body
     */
    void sendHtmlEmail(String to, String subject, String htmlBody);
}
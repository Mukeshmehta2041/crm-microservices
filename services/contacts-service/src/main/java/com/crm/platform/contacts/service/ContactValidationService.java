package com.crm.platform.contacts.service;

import com.crm.platform.contacts.dto.ContactRequest;
import com.crm.platform.contacts.exception.ContactBusinessException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ContactValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9\\s\\-\\(\\)]{7,20}$"
    );

    public void validateContactRequest(ContactRequest request, UUID tenantId) {
        if (request == null) {
            throw new ContactBusinessException("INVALID_REQUEST", "Contact request cannot be null");
        }

        // Validate required fields
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new ContactBusinessException("INVALID_FIRST_NAME", "First name is required");
        }

        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new ContactBusinessException("INVALID_LAST_NAME", "Last name is required");
        }

        // Validate email format
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
                throw new ContactBusinessException("INVALID_EMAIL", "Invalid email format");
            }
        }

        // Validate phone format
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (!PHONE_PATTERN.matcher(request.getPhone()).matches()) {
                throw new ContactBusinessException("INVALID_PHONE", "Invalid phone format");
            }
        }

        if (request.getMobile() != null && !request.getMobile().isEmpty()) {
            if (!PHONE_PATTERN.matcher(request.getMobile()).matches()) {
                throw new ContactBusinessException("INVALID_MOBILE", "Invalid mobile format");
            }
        }

        // Validate lead score range
        if (request.getLeadScore() != null && (request.getLeadScore() < 0 || request.getLeadScore() > 100)) {
            throw new ContactBusinessException("INVALID_LEAD_SCORE", "Lead score must be between 0 and 100");
        }

        // Validate field lengths
        if (request.getFirstName().length() > 100) {
            throw new ContactBusinessException("INVALID_FIRST_NAME", "First name cannot exceed 100 characters");
        }

        if (request.getLastName().length() > 100) {
            throw new ContactBusinessException("INVALID_LAST_NAME", "Last name cannot exceed 100 characters");
        }

        if (request.getEmail() != null && request.getEmail().length() > 255) {
            throw new ContactBusinessException("INVALID_EMAIL", "Email cannot exceed 255 characters");
        }

        if (request.getTitle() != null && request.getTitle().length() > 100) {
            throw new ContactBusinessException("INVALID_TITLE", "Title cannot exceed 100 characters");
        }

        if (request.getDepartment() != null && request.getDepartment().length() > 100) {
            throw new ContactBusinessException("INVALID_DEPARTMENT", "Department cannot exceed 100 characters");
        }
    }
}
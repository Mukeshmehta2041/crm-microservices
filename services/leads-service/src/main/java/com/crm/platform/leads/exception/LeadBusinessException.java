package com.crm.platform.leads.exception;

import com.crm.platform.common.exception.CrmBusinessException;

import java.util.Map;

/**
 * Business exception for lead-related operations
 */
public class LeadBusinessException extends CrmBusinessException {

    public LeadBusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public LeadBusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public LeadBusinessException(String errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }

    public LeadBusinessException(String errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(errorCode, message, details, cause);
    }
}
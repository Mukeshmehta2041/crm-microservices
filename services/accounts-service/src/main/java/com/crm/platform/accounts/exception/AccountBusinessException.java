package com.crm.platform.accounts.exception;

import com.crm.platform.common.exception.CrmBusinessException;

import java.util.Map;

/**
 * Concrete business exception for accounts service
 */
public class AccountBusinessException extends CrmBusinessException {

    public AccountBusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public AccountBusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public AccountBusinessException(String errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }

    public AccountBusinessException(String errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(errorCode, message, details, cause);
    }
}
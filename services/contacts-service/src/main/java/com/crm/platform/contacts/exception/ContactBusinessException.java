package com.crm.platform.contacts.exception;

import com.crm.platform.common.exception.CrmBusinessException;

import java.util.Map;

public class ContactBusinessException extends CrmBusinessException {

    public ContactBusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ContactBusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ContactBusinessException(String errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }

    public ContactBusinessException(String errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(errorCode, message, details, cause);
    }
}
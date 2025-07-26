package com.crm.platform.auth.exception;

import com.crm.platform.common.exception.CrmBusinessException;

public class AuthenticationException extends CrmBusinessException {

    public AuthenticationException(String message) {
        super("AUTH_ERROR", message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super("AUTH_ERROR", message, cause);
    }
}
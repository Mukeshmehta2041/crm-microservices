package com.crm.platform.common.exception;

import java.util.Map;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends CrmBusinessException {

  public ValidationException(String message) {
    super("VALIDATION_ERROR", message);
  }

  public ValidationException(String message, Throwable cause) {
    super("VALIDATION_ERROR", message, cause);
  }

  public ValidationException(String message, Map<String, Object> details) {
    super("VALIDATION_ERROR", message, details);
  }

  public ValidationException(String message, Map<String, Object> details, Throwable cause) {
    super("VALIDATION_ERROR", message, details, cause);
  }
}
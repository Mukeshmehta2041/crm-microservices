package com.crm.platform.common.exception;

/**
 * Business exception for backward compatibility
 * 
 * @deprecated Use CrmBusinessException instead
 */
@Deprecated
public class BusinessException extends CrmBusinessException {

  public BusinessException(String message) {
    super("BUSINESS_ERROR", message);
  }

  public BusinessException(String message, Throwable cause) {
    super("BUSINESS_ERROR", message, cause);
  }

  public BusinessException(String errorCode, String message) {
    super(errorCode, message);
  }

  public BusinessException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
package com.crm.platform.customobjects.exception;

import java.util.Map;

/**
 * Base exception for custom objects service business logic errors
 */
public abstract class CustomObjectBusinessException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> details;

    public CustomObjectBusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public CustomObjectBusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public CustomObjectBusinessException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public CustomObjectBusinessException(String message, String errorCode, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

/**
 * Exception thrown when a custom object is not found
 */
public static class CustomObjectNotFoundException extends CustomObjectBusinessException {
    public CustomObjectNotFoundException(String message) {
        super(message, "CUSTOM_OBJECT_NOT_FOUND");
    }

    public CustomObjectNotFoundException(String message, Map<String, Object> details) {
        super(message, "CUSTOM_OBJECT_NOT_FOUND", details);
    }
}

/**
 * Exception thrown when a custom object already exists
 */
public static class CustomObjectAlreadyExistsException extends CustomObjectBusinessException {
    public CustomObjectAlreadyExistsException(String message) {
        super(message, "CUSTOM_OBJECT_ALREADY_EXISTS");
    }

    public CustomObjectAlreadyExistsException(String message, Map<String, Object> details) {
        super(message, "CUSTOM_OBJECT_ALREADY_EXISTS", details);
    }
}

/**
 * Exception thrown when a custom field is not found
 */
public static class CustomFieldNotFoundException extends CustomObjectBusinessException {
    public CustomFieldNotFoundException(String message) {
        super(message, "CUSTOM_FIELD_NOT_FOUND");
    }

    public CustomFieldNotFoundException(String message, Map<String, Object> details) {
        super(message, "CUSTOM_FIELD_NOT_FOUND", details);
    }
}

/**
 * Exception thrown when a custom field already exists
 */
class CustomFieldAlreadyExistsException extends CustomObjectBusinessException {
    public CustomFieldAlreadyExistsException(String message) {
        super(message, "CUSTOM_FIELD_ALREADY_EXISTS");
    }

    public CustomFieldAlreadyExistsException(String message, Map<String, Object> details) {
        super(message, "CUSTOM_FIELD_ALREADY_EXISTS", details);
    }
}

/**
 * Exception thrown when a custom object record is not found
 */
class CustomObjectRecordNotFoundException extends CustomObjectBusinessException {
    public CustomObjectRecordNotFoundException(String message) {
        super(message, "CUSTOM_OBJECT_RECORD_NOT_FOUND");
    }

    public CustomObjectRecordNotFoundException(String message, Map<String, Object> details) {
        super(message, "CUSTOM_OBJECT_RECORD_NOT_FOUND", details);
    }
}

/**
 * Exception thrown when field validation fails
 */
class CustomFieldValidationException extends CustomObjectBusinessException {
    public CustomFieldValidationException(String message) {
        super(message, "CUSTOM_FIELD_VALIDATION_ERROR");
    }

    public CustomFieldValidationException(String message, Map<String, Object> details) {
        super(message, "CUSTOM_FIELD_VALIDATION_ERROR", details);
    }
}

/**
 * Exception thrown when a unique constraint is violated
 */
class UniqueConstraintViolationException extends CustomObjectBusinessException {
    public UniqueConstraintViolationException(String message) {
        super(message, "UNIQUE_CONSTRAINT_VIOLATION");
    }

    public UniqueConstraintViolationException(String message, Map<String, Object> details) {
        super(message, "UNIQUE_CONSTRAINT_VIOLATION", details);
    }
}

/**
 * Exception thrown when required field validation fails
 */
class RequiredFieldValidationException extends CustomObjectBusinessException {
    public RequiredFieldValidationException(String message) {
        super(message, "REQUIRED_FIELD_VALIDATION_ERROR");
    }

    public RequiredFieldValidationException(String message, Map<String, Object> details) {
        super(message, "REQUIRED_FIELD_VALIDATION_ERROR", details);
    }
}

/**
 * Exception thrown when dynamic query generation fails
 */
class DynamicQueryException extends CustomObjectBusinessException {
    public DynamicQueryException(String message) {
        super(message, "DYNAMIC_QUERY_ERROR");
    }

    public DynamicQueryException(String message, Throwable cause) {
        super(message, "DYNAMIC_QUERY_ERROR", cause);
    }

    public DynamicQueryException(String message, Map<String, Object> details) {
        super(message, "DYNAMIC_QUERY_ERROR", details);
    }
}
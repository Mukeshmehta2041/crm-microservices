package com.crm.platform.common.exception;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.ErrorDetail;
import com.crm.platform.common.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Global exception handler for consistent error responses across all services
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleEntityNotFound(
      EntityNotFoundException ex, WebRequest request) {

    logger.warn("Entity not found: {}", ex.getMessage());

    List<ApiError> errors = List.of(
        new ApiError("ENTITY_NOT_FOUND", ex.getMessage(), null, null, null, null));

    ApiResponse<Object> response = ApiResponse.error(errors);
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
      IllegalArgumentException ex, WebRequest request) {

    logger.warn("Illegal argument: {}", ex.getMessage());

    List<ApiError> errors = List.of(
        new ApiError("INVALID_ARGUMENT", ex.getMessage(), null, null, null, null));

    ApiResponse<Object> response = ApiResponse.error(errors);
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidationErrors(
      MethodArgumentNotValidException ex, WebRequest request) {

    logger.warn("Validation failed: {}", ex.getMessage());

    List<ErrorDetail> errors = new ArrayList<>();

    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.add(new ErrorDetail(
          "VALIDATION_ERROR",
          error.getDefaultMessage(),
          error.getField(),
          error.getRejectedValue() != null ? error.getRejectedValue().toString() : null,
          error.getCode()));
    }

    ApiResponse<Object> response = ApiResponse.error(convertToApiErrors(errors));
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ApiResponse<Object>> handleBindException(
      BindException ex, WebRequest request) {

    logger.warn("Bind exception: {}", ex.getMessage());

    List<ErrorDetail> errors = new ArrayList<>();

    for (FieldError error : ex.getFieldErrors()) {
      errors.add(new ErrorDetail(
          "BIND_ERROR",
          error.getDefaultMessage(),
          error.getField(),
          error.getRejectedValue() != null ? error.getRejectedValue().toString() : null,
          error.getCode()));
    }

    ApiResponse<Object> response = ApiResponse.error(convertToApiErrors(errors));
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request) {

    logger.warn("Constraint violation: {}", ex.getMessage());

    List<ErrorDetail> errors = new ArrayList<>();
    Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

    for (ConstraintViolation<?> violation : violations) {
      String field = violation.getPropertyPath().toString();
      errors.add(new ErrorDetail(
          "CONSTRAINT_VIOLATION",
          violation.getMessage(),
          field,
          violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : null,
          violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()));
    }

    ApiResponse<Object> response = ApiResponse.error(convertToApiErrors(errors));
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, WebRequest request) {

    logger.warn("Type mismatch: {}", ex.getMessage());

    String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
        ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

    List<ErrorDetail> errors = List.of(
        new ErrorDetail("TYPE_MISMATCH", message, ex.getName(),
            ex.getValue() != null ? ex.getValue().toString() : null, null));

    ApiResponse<Object> response = ApiResponse.error(convertToApiErrors(errors));
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResponse<Object>> handleMissingParameter(
      MissingServletRequestParameterException ex, WebRequest request) {

    logger.warn("Missing parameter: {}", ex.getMessage());

    List<ErrorDetail> errors = List.of(
        new ErrorDetail("MISSING_PARAMETER", ex.getMessage(), ex.getParameterName(), null, null));

    ApiResponse<Object> response = ApiResponse.error(convertToApiErrors(errors));
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(
      HttpMessageNotReadableException ex, WebRequest request) {

    logger.warn("Message not readable: {}", ex.getMessage());

    List<ApiError> errors = List.of(
        new ApiError("INVALID_REQUEST_BODY", "Invalid JSON format or missing required fields", null, null, null, null));

    ApiResponse<Object> response = ApiResponse.error(errors);
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, WebRequest request) {

    logger.warn("Method not supported: {}", ex.getMessage());

    String message = String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
        ex.getMethod(), String.join(", ", ex.getSupportedMethods()));

    List<ApiError> errors = List.of(
        new ApiError("METHOD_NOT_SUPPORTED", message, null, null, null, null));

    ApiResponse<Object> response = ApiResponse.error(errors);
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Object>> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, WebRequest request) {

    logger.warn("Media type not supported: {}", ex.getMessage());

    String message = String.format("Media type '%s' is not supported. Supported media types: %s",
        ex.getContentType(), ex.getSupportedMediaTypes());

    List<ApiError> errors = List.of(
        new ApiError("MEDIA_TYPE_NOT_SUPPORTED", message, null, null, null, null));

    ApiResponse<Object> response = ApiResponse.error(errors);
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleNoHandlerFound(
      NoHandlerFoundException ex, WebRequest request) {

    logger.warn("No handler found: {}", ex.getMessage());

    String message = String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL());

    List<ApiError> errors = List.of(
        new ApiError("ENDPOINT_NOT_FOUND", message, null, null, null, null));

    ApiResponse<Object> response = ApiResponse.error(errors);
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleGenericException(
      Exception ex, WebRequest request) {

    logger.error("Unexpected error occurred", ex);

    List<ApiError> errors = List.of(
        new ApiError("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.", null, null,
            null, null));

    ApiResponse<Object> response = ApiResponse.error(errors);
    addRequestMetadata(response, request);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  private void addRequestMetadata(ApiResponse<Object> response, WebRequest request) {
    response.getMeta().setTimestamp(Instant.now().toString());

    // Add request ID if available
    String requestId = request.getHeader("X-Request-ID");
    if (requestId != null) {
      response.getMeta().setRequestId(requestId);
    }

    // Add API version if available
    String apiVersion = request.getHeader("X-API-Version");
    if (apiVersion != null) {
      response.getMeta().setVersion(apiVersion);
    }
  }

  private List<ApiError> convertToApiErrors(List<ErrorDetail> errorDetails) {
    return errorDetails.stream()
        .map(error -> new ApiError(
            error.getCode(),
            error.getMessage(),
            error.getField(),
            error.getValue(),
            error.getConstraint(),
            null))
        .toList();
  }
}
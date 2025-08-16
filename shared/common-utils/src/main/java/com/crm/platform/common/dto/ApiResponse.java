package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private boolean success;
  private T data;
  private ApiResponseMeta meta;
  private List<ApiError> errors;

  public ApiResponse() {
  }

  public ApiResponse(boolean success, T data, ApiResponseMeta meta, List<ApiError> errors) {
    this.success = success;
    this.data = data;
    this.meta = meta;
    this.errors = errors;
  }

  // Static factory methods
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, createMeta(), null);
  }

  public static <T> ApiResponse<T> success(T data, ApiResponseMeta meta) {
    return new ApiResponse<>(true, data, meta, null);
  }

  public static <T> ApiResponse<T> success(T data, String message) {
    ApiResponseMeta meta = createMeta();
    meta.setMessage(message);
    return new ApiResponse<>(true, data, meta, null);
  }

  public static <T> ApiResponse<T> error(List<ApiError> errors) {
    return new ApiResponse<>(false, null, createMeta(), errors);
  }

  public static <T> ApiResponse<T> error(ApiError error) {
    return new ApiResponse<>(false, null, createMeta(), List.of(error));
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    ApiError error = new ApiError(code, message, null, null, null, null);
    return new ApiResponse<>(false, null, createMeta(), List.of(error));
  }

  public static <T> ApiResponse<T> error(String message) {
    ApiError error = new ApiError("GENERAL_ERROR", message, null, null, null, null);
    return new ApiResponse<>(false, null, createMeta(), List.of(error));
  }

  public static <T> ApiResponse<T> errorWithDetails(List<ErrorDetail> errors) {
    List<ApiError> apiErrors = errors.stream()
        .map(error -> new ApiError(error.getCode(), error.getMessage(), error.getField(), error.getValue(),
            error.getConstraint(), null))
        .toList();
    return new ApiResponse<>(false, null, createMeta(), apiErrors);
  }

  private static ApiResponseMeta createMeta() {
    return new ApiResponseMeta(
        Instant.now().toString(),
        "v1",
        generateRequestId(),
        null,
        null,
        null);
  }

  private static String generateRequestId() {
    return "req_" + System.currentTimeMillis() + "_" +
        Integer.toHexString((int) (Math.random() * 0x10000));
  }

  // Getters and Setters
  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public ApiResponseMeta getMeta() {
    return meta;
  }

  public void setMeta(ApiResponseMeta meta) {
    this.meta = meta;
  }

  public List<ApiError> getErrors() {
    return errors;
  }

  public void setErrors(List<ApiError> errors) {
    this.errors = errors;
  }
}
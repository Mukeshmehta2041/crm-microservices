package com.crm.platform.common.controller;

import com.crm.platform.common.annotation.ApiVersion;
import com.crm.platform.common.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import com.crm.platform.common.dto.PaginationMeta;

/**
 * Base controller providing common CRUD operations and advanced features
 */
@Validated
@ApiVersion("1")
public abstract class BaseController<T, ID> {

  // Abstract methods to be implemented by concrete controllers
  protected abstract T findById(ID id);

  protected abstract Page<T> findAll(PageRequest pageRequest);

  protected abstract T createInternal(T entity);

  protected abstract T updateInternal(ID id, T entity);

  protected abstract void deleteInternal(ID id);

  protected abstract BulkOperationResponse<T> bulkOperationInternal(BulkOperationRequest<T> request);

  protected abstract Page<T> search(String query, List<String> fields, PageRequest pageRequest);

  // Standard CRUD endpoints

  @GetMapping("/{id}")
  @Operation(summary = "Get entity by ID", description = "Retrieve a single entity by its unique identifier")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Entity found", content = @Content(schema = @Schema(implementation = com.crm.platform.common.dto.ApiResponse.class))),
      @ApiResponse(responseCode = "404", description = "Entity not found"),
      @ApiResponse(responseCode = "400", description = "Invalid ID format")
  })
  public ResponseEntity<com.crm.platform.common.dto.ApiResponse<T>> getById(
      @Parameter(description = "Entity ID", required = true) @PathVariable ID id) {

    T entity = findById(id);
    return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(entity));
  }

  @GetMapping
  @Operation(summary = "Get all entities", description = "Retrieve entities with pagination, filtering, and sorting")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Entities retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters")
  })
  public ResponseEntity<com.crm.platform.common.dto.ApiResponse<List<T>>> getAll(
      @Parameter(description = "Page number (1-based)", example = "1") @RequestParam(defaultValue = "1") @Min(1) Integer page,

      @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(1000) Integer limit,

      @Parameter(description = "Sort criteria (field:direction)", example = "name:asc,createdAt:desc") @RequestParam(required = false) List<String> sort,

      @Parameter(description = "Search term") @RequestParam(required = false) String search,

      @Parameter(description = "Fields to search in") @RequestParam(required = false) List<String> searchFields,

      @Parameter(description = "Fields to include in response") @RequestParam(required = false) List<String> fields,

      @Parameter(description = "Related entities to include") @RequestParam(required = false) List<String> include) {

    PageRequest pageRequest = new PageRequest();
    pageRequest.setPage(page);
    pageRequest.setLimit(limit);
    pageRequest.setSearch(search);
    pageRequest.setSearchFields(searchFields);
    pageRequest.setFields(fields);
    pageRequest.setInclude(include);

    if (sort != null && !sort.isEmpty()) {
      pageRequest.setSort(SortCriteria.of(sort.toArray(new String[0])));
    }

    Page<T> result = findAll(pageRequest);

    com.crm.platform.common.dto.ApiResponse<List<T>> response = com.crm.platform.common.dto.ApiResponse
        .success(result.getContent());

    // Set pagination metadata
    PaginationMeta pagination = new PaginationMeta(page, limit, result.getTotalElements());
    response.getMeta().setPagination(pagination);

    return ResponseEntity.ok(response);
  }

  @PostMapping
  @Operation(summary = "Create new entity", description = "Create a new entity")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Entity created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid entity data"),
      @ApiResponse(responseCode = "409", description = "Entity already exists")
  })
  public ResponseEntity<com.crm.platform.common.dto.ApiResponse<T>> create(
      @Parameter(description = "Entity data", required = true) @Valid @RequestBody T entity) {

    T created = createInternal(entity);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(com.crm.platform.common.dto.ApiResponse.success(created));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update entity", description = "Update an existing entity")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Entity updated successfully"),
      @ApiResponse(responseCode = "404", description = "Entity not found"),
      @ApiResponse(responseCode = "400", description = "Invalid entity data")
  })
  public ResponseEntity<com.crm.platform.common.dto.ApiResponse<T>> update(
      @Parameter(description = "Entity ID", required = true) @PathVariable ID id,

      @Parameter(description = "Updated entity data", required = true) @Valid @RequestBody T entity) {

    T updated = updateInternal(id, entity);
    return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(updated));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete entity", description = "Delete an entity by ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Entity deleted successfully"),
      @ApiResponse(responseCode = "404", description = "Entity not found")
  })
  public ResponseEntity<Void> delete(
      @Parameter(description = "Entity ID", required = true) @PathVariable ID id) {

    deleteInternal(id);
    return ResponseEntity.noContent().build();
  }

  // Advanced endpoints

  @PostMapping("/search")
  @Operation(summary = "Advanced search", description = "Search entities with complex criteria")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Search completed successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid search criteria")
  })
  public ResponseEntity<com.crm.platform.common.dto.ApiResponse<List<T>>> search(
      @Parameter(description = "Search request", required = true) @Valid @RequestBody PageRequest searchRequest) {

    Page<T> result;

    if (searchRequest.getSearch() != null && !searchRequest.getSearch().trim().isEmpty()) {
      result = search(searchRequest.getSearch(), searchRequest.getSearchFields(), searchRequest);
    } else {
      result = findAll(searchRequest);
    }

    com.crm.platform.common.dto.ApiResponse<List<T>> response = com.crm.platform.common.dto.ApiResponse
        .success(result.getContent());

    // Set pagination metadata
    PaginationMeta pagination = new PaginationMeta(searchRequest.getPage(), searchRequest.getLimit(),
        result.getTotalElements());
    response.getMeta().setPagination(pagination);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/bulk")
  @Operation(summary = "Bulk operations", description = "Perform bulk create, update, or delete operations")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Bulk operation completed"),
      @ApiResponse(responseCode = "202", description = "Bulk operation accepted (async)"),
      @ApiResponse(responseCode = "400", description = "Invalid bulk request")
  })
  public ResponseEntity<com.crm.platform.common.dto.ApiResponse<BulkOperationResponse<T>>> bulkOperation(
      @Parameter(description = "Bulk operation request", required = true) @Valid @RequestBody BulkOperationRequest<T> request) {

    BulkOperationResponse<T> response = bulkOperationInternal(request);

    HttpStatus status = request.getAsync() ? HttpStatus.ACCEPTED : HttpStatus.OK;

    return ResponseEntity.status(status)
        .body(com.crm.platform.common.dto.ApiResponse.success(response));
  }

  @GetMapping("/bulk/{jobId}")
  @Operation(summary = "Get bulk operation status", description = "Get the status of a bulk operation")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Bulk operation status retrieved"),
      @ApiResponse(responseCode = "404", description = "Bulk operation not found")
  })
  public ResponseEntity<com.crm.platform.common.dto.ApiResponse<BulkOperationResponse<T>>> getBulkOperationStatus(
      @Parameter(description = "Bulk operation job ID", required = true) @PathVariable UUID jobId) {

    // This would typically be implemented by concrete controllers
    // or delegated to a bulk operation service
    throw new UnsupportedOperationException("Bulk operation status tracking not implemented");
  }

  // Utility methods for concrete controllers

  protected com.crm.platform.common.dto.ApiResponse<T> successResponse(T data) {
    return com.crm.platform.common.dto.ApiResponse.success(data);
  }

  protected com.crm.platform.common.dto.ApiResponse<T> successResponse(T data, String message) {
    return com.crm.platform.common.dto.ApiResponse.success(data, message);
  }

  protected com.crm.platform.common.dto.ApiResponse<String> successResponse(String message) {
    return com.crm.platform.common.dto.ApiResponse.success(message);
  }

  protected com.crm.platform.common.dto.ApiResponse<T> errorResponse(String message) {
    return com.crm.platform.common.dto.ApiResponse.error(message);
  }

  protected com.crm.platform.common.dto.ApiResponse<T> errorResponse(List<ErrorDetail> errors) {
    return com.crm.platform.common.dto.ApiResponse.errorWithDetails(errors);
  }
}
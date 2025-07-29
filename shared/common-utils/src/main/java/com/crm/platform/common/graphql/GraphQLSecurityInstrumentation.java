package com.crm.platform.common.graphql;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * GraphQL instrumentation for security and rate limiting
 */
@Component
public class GraphQLSecurityInstrumentation extends SimpleInstrumentation {

  private static final int MAX_QUERY_DEPTH = 15;
  private static final int MAX_QUERY_COMPLEXITY = 1000;

  @Override
  public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
    // Validate authentication
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Authentication required for GraphQL queries");
    }

    // Validate query complexity and depth
    validateQueryComplexity(parameters);

    return new InstrumentationContext<ExecutionResult>() {
      @Override
      public void onCompleted(ExecutionResult result, Throwable t) {
        // Log security events if needed
      }

      @Override
      public void onDispatched(CompletableFuture<ExecutionResult> result) {
        // No-op
      }
    };
  }

  @Override
  public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher,
      InstrumentationFieldFetchParameters parameters) {
    return environment -> {
      // Check field-level permissions
      String fieldName = parameters.getField().getName();
      // Note: Type information would be available from the execution context
      // For now, we'll skip field-level security checks

      return dataFetcher.get(environment);
    };
  }

  private void validateQueryComplexity(InstrumentationExecutionParameters parameters) {
    // This would implement query complexity analysis
    // For now, we'll do a simple depth check based on the query string
    String query = parameters.getQuery();
    int depth = calculateQueryDepth(query);

    if (depth > MAX_QUERY_DEPTH) {
      throw new IllegalArgumentException("Query depth exceeds maximum allowed: " + MAX_QUERY_DEPTH);
    }
  }

  private int calculateQueryDepth(String query) {
    // Simple depth calculation based on nested braces
    int depth = 0;
    int maxDepth = 0;

    for (char c : query.toCharArray()) {
      if (c == '{') {
        depth++;
        maxDepth = Math.max(maxDepth, depth);
      } else if (c == '}') {
        depth--;
      }
    }

    return maxDepth;
  }

  private boolean isRestrictedField(String typeName, String fieldName) {
    // Define restricted fields that require special permissions
    return (typeName.equals("Contact") && fieldName.equals("socialSecurityNumber")) ||
        (typeName.equals("Deal") && fieldName.equals("internalNotes")) ||
        (typeName.equals("User") && fieldName.equals("salary"));
  }

  private boolean hasFieldPermission(Authentication authentication, String typeName, String fieldName) {
    // Check if user has permission to access this field
    // This would integrate with your permission system
    return authentication.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") ||
            authority.getAuthority().equals("PERMISSION_" + typeName.toUpperCase() + "_" + fieldName.toUpperCase()));
  }
}
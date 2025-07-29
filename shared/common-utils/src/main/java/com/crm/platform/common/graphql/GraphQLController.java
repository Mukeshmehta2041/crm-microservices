package com.crm.platform.common.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL HTTP endpoint controller
 */
@RestController
@RequestMapping("/graphql")
@Tag(name = "GraphQL", description = "GraphQL API endpoint for flexible data querying")
public class GraphQLController {
    
    private final GraphQL graphQL;
    private final DataLoaderRegistry dataLoaderRegistry;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public GraphQLController(GraphQL graphQL, DataLoaderRegistry dataLoaderRegistry, ObjectMapper objectMapper) {
        this.graphQL = graphQL;
        this.dataLoaderRegistry = dataLoaderRegistry;
        this.objectMapper = objectMapper;
    }
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Execute GraphQL query", 
               description = "Execute a GraphQL query, mutation, or subscription")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GraphQL query executed successfully",
                    content = @Content(schema = @Schema(implementation = GraphQLResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid GraphQL query"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public CompletableFuture<ResponseEntity<Object>> graphql(
            @RequestBody GraphQLRequest request,
            HttpServletRequest httpRequest) {
        
        // Build execution input
        ExecutionInput.Builder inputBuilder = ExecutionInput.newExecutionInput()
                .query(request.getQuery())
                .dataLoaderRegistry(dataLoaderRegistry)
                .context(buildGraphQLContext(httpRequest));
        
        // Add variables if present
        if (request.getVariables() != null) {
            inputBuilder.variables(request.getVariables());
        }
        
        // Add operation name if present
        if (request.getOperationName() != null) {
            inputBuilder.operationName(request.getOperationName());
        }
        
        ExecutionInput executionInput = inputBuilder.build();
        
        // Execute query asynchronously
        return graphQL.executeAsync(executionInput)
                .thenApply(this::buildResponse)
                .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Execute GraphQL query via GET", 
               description = "Execute a simple GraphQL query via GET request")
    public CompletableFuture<ResponseEntity<Object>> graphqlGet(
            @RequestParam String query,
            @RequestParam(required = false) String variables,
            @RequestParam(required = false) String operationName,
            HttpServletRequest httpRequest) {
        
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(query);
        request.setOperationName(operationName);
        
        if (variables != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> variablesMap = objectMapper.readValue(variables, Map.class);
                request.setVariables(variablesMap);
            } catch (Exception e) {
                return CompletableFuture.completedFuture(
                        ResponseEntity.badRequest().body(Map.of("error", "Invalid variables JSON")));
            }
        }
        
        return graphql(request, httpRequest);
    }
    
    private GraphQLContext buildGraphQLContext(HttpServletRequest request) {
        GraphQLContext context = new GraphQLContext();
        
        // Add authentication information
        context.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
        
        // Add request information
        context.setRequestId(request.getHeader("X-Request-ID"));
        context.setUserAgent(request.getHeader("User-Agent"));
        context.setRemoteAddress(request.getRemoteAddr());
        
        // Add tenant information
        context.setTenantId(request.getHeader("X-Tenant-ID"));
        
        return context;
    }
    
    private Object buildResponse(ExecutionResult executionResult) {
        GraphQLResponse response = new GraphQLResponse();
        
        // Set data
        response.setData(executionResult.getData());
        
        // Set errors if any
        if (executionResult.getErrors() != null && !executionResult.getErrors().isEmpty()) {
            response.setErrors(executionResult.getErrors());
        }
        
        // Set extensions
        if (executionResult.getExtensions() != null && !executionResult.getExtensions().isEmpty()) {
            response.setExtensions(executionResult.getExtensions());
        }
        
        return response;
    }
    
    // Request/Response DTOs
    public static class GraphQLRequest {
        private String query;
        private Map<String, Object> variables;
        private String operationName;
        
        // Getters and setters
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
        
        public Map<String, Object> getVariables() {
            return variables;
        }
        
        public void setVariables(Map<String, Object> variables) {
            this.variables = variables;
        }
        
        public String getOperationName() {
            return operationName;
        }
        
        public void setOperationName(String operationName) {
            this.operationName = operationName;
        }
    }
    
    public static class GraphQLResponse {
        private Object data;
        private Object errors;
        private Map<String, Object> extensions;
        
        // Getters and setters
        public Object getData() {
            return data;
        }
        
        public void setData(Object data) {
            this.data = data;
        }
        
        public Object getErrors() {
            return errors;
        }
        
        public void setErrors(Object errors) {
            this.errors = errors;
        }
        
        public Map<String, Object> getExtensions() {
            return extensions;
        }
        
        public void setExtensions(Map<String, Object> extensions) {
            this.extensions = extensions;
        }
    }
    
    public static class GraphQLContext {
        private Object authentication;
        private String requestId;
        private String userAgent;
        private String remoteAddress;
        private String tenantId;
        
        // Getters and setters
        public Object getAuthentication() {
            return authentication;
        }
        
        public void setAuthentication(Object authentication) {
            this.authentication = authentication;
        }
        
        public String getRequestId() {
            return requestId;
        }
        
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
        
        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
        
        public String getRemoteAddress() {
            return remoteAddress;
        }
        
        public void setRemoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
        }
        
        public String getTenantId() {
            return tenantId;
        }
        
        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}
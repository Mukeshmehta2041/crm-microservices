package com.crm.platform.auth.controller;

import com.crm.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/docs")
@Tag(name = "API Documentation", description = "API documentation and examples")
@Hidden // Hide from main API docs to avoid recursion
public class ApiDocumentationController {

    @GetMapping("/examples/authentication")
    @Operation(
        summary = "Get authentication examples",
        description = "Retrieve code examples for different authentication methods"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Examples retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthenticationExamples() {
        Map<String, Object> examples = Map.of(
            "jwt_login", Map.of(
                "description", "Login with username/password to get JWT token",
                "curl", """
                    curl -X POST https://api.crmplatform.com/api/v1/auth/login \\
                      -H "Content-Type: application/json" \\
                      -H "X-Tenant-ID: your-tenant-id" \\
                      -d '{
                        "usernameOrEmail": "user@example.com",
                        "password": "your-password"
                      }'
                    """,
                "javascript", """
                    const response = await fetch('https://api.crmplatform.com/api/v1/auth/login', {
                      method: 'POST',
                      headers: {
                        'Content-Type': 'application/json',
                        'X-Tenant-ID': 'your-tenant-id'
                      },
                      body: JSON.stringify({
                        usernameOrEmail: 'user@example.com',
                        password: 'your-password'
                      })
                    });
                    const data = await response.json();
                    const token = data.data.accessToken;
                    """,
                "python", """
                    import requests
                    
                    response = requests.post(
                        'https://api.crmplatform.com/api/v1/auth/login',
                        headers={
                            'Content-Type': 'application/json',
                            'X-Tenant-ID': 'your-tenant-id'
                        },
                        json={
                            'usernameOrEmail': 'user@example.com',
                            'password': 'your-password'
                        }
                    )
                    token = response.json()['data']['accessToken']
                    """
            ),
            "oauth2_flow", Map.of(
                "description", "OAuth2 authorization code flow",
                "step1", "Redirect user to authorization endpoint",
                "step1_url", "https://api.crmplatform.com/api/v1/auth/oauth2/authorize?client_id=your-client-id&response_type=code&redirect_uri=your-redirect-uri&scope=read write",
                "step2", "Exchange authorization code for tokens",
                "step2_curl", """
                    curl -X POST https://api.crmplatform.com/api/v1/auth/oauth2/token \\
                      -H "Content-Type: application/x-www-form-urlencoded" \\
                      -d "grant_type=authorization_code&code=auth-code&client_id=your-client-id&client_secret=your-client-secret&redirect_uri=your-redirect-uri"
                    """
            ),
            "using_token", Map.of(
                "description", "Using JWT token for API calls",
                "curl", """
                    curl -X GET https://api.crmplatform.com/api/v1/auth/user/profile \\
                      -H "Authorization: Bearer your-jwt-token" \\
                      -H "X-Tenant-ID: your-tenant-id"
                    """,
                "javascript", """
                    const response = await fetch('https://api.crmplatform.com/api/v1/auth/user/profile', {
                      headers: {
                        'Authorization': 'Bearer ' + token,
                        'X-Tenant-ID': 'your-tenant-id'
                      }
                    });
                    """
            )
        );

        return ResponseEntity.ok(ApiResponse.success(examples));
    }

    @GetMapping("/examples/mfa")
    @Operation(
        summary = "Get MFA examples",
        description = "Retrieve code examples for Multi-Factor Authentication"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMfaExamples() {
        Map<String, Object> examples = Map.of(
            "setup_mfa", Map.of(
                "description", "Set up TOTP-based MFA",
                "curl", """
                    curl -X POST https://api.crmplatform.com/api/v1/auth/mfa/setup \\
                      -H "Authorization: Bearer your-jwt-token" \\
                      -H "Content-Type: application/json" \\
                      -d '{
                        "method": "TOTP"
                      }'
                    """,
                "response", """
                    {
                      "success": true,
                      "data": {
                        "secret": "JBSWY3DPEHPK3PXP",
                        "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
                        "backupCodes": ["123456", "789012", ...]
                      }
                    }
                    """
            ),
            "verify_mfa", Map.of(
                "description", "Verify MFA code during login",
                "curl", """
                    curl -X POST https://api.crmplatform.com/api/v1/auth/mfa/verify \\
                      -H "Content-Type: application/json" \\
                      -d '{
                        "mfaToken": "mfa-challenge-token",
                        "code": "123456"
                      }'
                    """
            )
        );

        return ResponseEntity.ok(ApiResponse.success(examples));
    }

    @GetMapping("/examples/session-management")
    @Operation(
        summary = "Get session management examples",
        description = "Retrieve code examples for session management"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionExamples() {
        Map<String, Object> examples = Map.of(
            "list_sessions", Map.of(
                "description", "List active user sessions",
                "curl", """
                    curl -X GET https://api.crmplatform.com/api/v1/auth/sessions \\
                      -H "Authorization: Bearer your-jwt-token"
                    """
            ),
            "terminate_session", Map.of(
                "description", "Terminate a specific session",
                "curl", """
                    curl -X DELETE https://api.crmplatform.com/api/v1/auth/sessions/session-id \\
                      -H "Authorization: Bearer your-jwt-token"
                    """
            ),
            "terminate_all_sessions", Map.of(
                "description", "Terminate all sessions except current",
                "curl", """
                    curl -X POST https://api.crmplatform.com/api/v1/auth/sessions/terminate-all \\
                      -H "Authorization: Bearer your-jwt-token"
                    """
            )
        );

        return ResponseEntity.ok(ApiResponse.success(examples));
    }

    @GetMapping("/troubleshooting")
    @Operation(
        summary = "Get troubleshooting guide",
        description = "Retrieve common issues and solutions"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTroubleshootingGuide() {
        Map<String, Object> guide = Map.of(
            "common_errors", List.of(
                Map.of(
                    "error", "INVALID_CREDENTIALS",
                    "description", "Username/password authentication failed",
                    "solutions", List.of(
                        "Verify username and password are correct",
                        "Check if account is locked (check account lockout status)",
                        "Ensure tenant context is provided",
                        "Verify email is verified if required"
                    )
                ),
                Map.of(
                    "error", "TOKEN_EXPIRED",
                    "description", "JWT token has expired",
                    "solutions", List.of(
                        "Use refresh token to get new access token",
                        "Re-authenticate if refresh token is also expired",
                        "Check token expiration time in JWT payload"
                    )
                ),
                Map.of(
                    "error", "MFA_REQUIRED",
                    "description", "Multi-factor authentication is required",
                    "solutions", List.of(
                        "Complete MFA challenge using the provided MFA token",
                        "Use backup codes if TOTP is not available",
                        "Contact administrator if MFA device is lost"
                    )
                ),
                Map.of(
                    "error", "RATE_LIMIT_EXCEEDED",
                    "description", "Too many requests in a short time",
                    "solutions", List.of(
                        "Wait for rate limit window to reset",
                        "Implement exponential backoff in your client",
                        "Check rate limit headers in response",
                        "Contact support for rate limit increases"
                    )
                ),
                Map.of(
                    "error", "TENANT_CONTEXT_REQUIRED",
                    "description", "Tenant context is missing or invalid",
                    "solutions", List.of(
                        "Provide X-Tenant-ID header with valid tenant UUID",
                        "Use tenant subdomain in API URL",
                        "Ensure JWT token contains tenant information",
                        "Verify tenant is active and not suspended"
                    )
                )
            ),
            "debugging_tips", List.of(
                "Check HTTP status codes for error categories",
                "Examine error response body for detailed error information",
                "Use request ID from response meta for support requests",
                "Enable debug logging in your HTTP client",
                "Verify all required headers are present",
                "Test with Postman collection for reference implementation"
            ),
            "support_resources", Map.of(
                "documentation", "https://docs.crmplatform.com",
                "status_page", "https://status.crmplatform.com",
                "support_email", "support@crmplatform.com",
                "github_issues", "https://github.com/crmplatform/api/issues"
            )
        );

        return ResponseEntity.ok(ApiResponse.success(guide));
    }

    @GetMapping("/error-codes")
    @Operation(
        summary = "Get error codes reference",
        description = "Retrieve comprehensive list of error codes and their meanings"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getErrorCodes() {
        Map<String, Object> errorCodes = Map.of(
            "authentication_errors", Map.of(
                "INVALID_CREDENTIALS", "Username or password is incorrect",
                "ACCOUNT_LOCKED", "Account is temporarily locked due to failed login attempts",
                "ACCOUNT_DISABLED", "Account has been disabled by administrator",
                "EMAIL_NOT_VERIFIED", "Email address must be verified before login",
                "MFA_REQUIRED", "Multi-factor authentication is required",
                "MFA_INVALID", "MFA code is invalid or expired",
                "TOKEN_EXPIRED", "JWT token has expired",
                "TOKEN_INVALID", "JWT token is malformed or invalid",
                "REFRESH_TOKEN_EXPIRED", "Refresh token has expired"
            ),
            "authorization_errors", Map.of(
                "INSUFFICIENT_PERMISSIONS", "User lacks required permissions for this operation",
                "INVALID_SCOPE", "OAuth2 scope is invalid or insufficient",
                "TENANT_ACCESS_DENIED", "Access to tenant is denied",
                "RESOURCE_NOT_FOUND", "Requested resource does not exist",
                "OPERATION_NOT_ALLOWED", "Operation is not allowed in current context"
            ),
            "validation_errors", Map.of(
                "REQUIRED_FIELD", "Required field is missing",
                "INVALID_FORMAT", "Field format is invalid",
                "CONSTRAINT_VIOLATION", "Field violates constraints",
                "DUPLICATE_VALUE", "Value already exists",
                "INVALID_LENGTH", "Field length is invalid"
            ),
            "rate_limiting_errors", Map.of(
                "RATE_LIMIT_EXCEEDED", "Rate limit exceeded for this endpoint",
                "IP_BLOCKED", "IP address is temporarily blocked",
                "CAPTCHA_REQUIRED", "CAPTCHA verification is required",
                "SUSPICIOUS_ACTIVITY", "Suspicious activity detected"
            ),
            "system_errors", Map.of(
                "INTERNAL_SERVER_ERROR", "An internal server error occurred",
                "SERVICE_UNAVAILABLE", "Service is temporarily unavailable",
                "TIMEOUT", "Request timed out",
                "MAINTENANCE_MODE", "System is in maintenance mode"
            )
        );

        return ResponseEntity.ok(ApiResponse.success(errorCodes));
    }

    @GetMapping("/postman-collection")
    @Operation(
        summary = "Get Postman collection",
        description = "Retrieve Postman collection for API testing"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostmanCollection() {
        Map<String, Object> collection = Map.of(
            "info", Map.of(
                "name", "CRM Platform Authentication Service",
                "description", "Comprehensive API collection for authentication service",
                "version", "1.0.0"
            ),
            "download_links", Map.of(
                "auth_service", "/api/v1/auth/docs/postman/auth-service.json",
                "user_management", "/api/v1/auth/docs/postman/user-management.json",
                "complete_collection", "/api/v1/auth/docs/postman/complete.json"
            ),
            "environment_variables", Map.of(
                "base_url", "{{base_url}}",
                "tenant_id", "{{tenant_id}}",
                "access_token", "{{access_token}}",
                "refresh_token", "{{refresh_token}}"
            ),
            "setup_instructions", List.of(
                "Import the collection into Postman",
                "Set up environment variables",
                "Run the 'Login' request to get access token",
                "Access token will be automatically set for subsequent requests",
                "Use 'Refresh Token' request when access token expires"
            )
        );

        return ResponseEntity.ok(ApiResponse.success(collection));
    }

    @GetMapping("/sdk-examples")
    @Operation(
        summary = "Get SDK examples",
        description = "Retrieve examples for different programming languages and SDKs"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSdkExamples() {
        Map<String, Object> examples = Map.of(
            "javascript", Map.of(
                "installation", "npm install @crmplatform/auth-sdk",
                "basic_usage", """
                    import { AuthClient } from '@crmplatform/auth-sdk';
                    
                    const client = new AuthClient({
                      baseUrl: 'https://api.crmplatform.com',
                      tenantId: 'your-tenant-id'
                    });
                    
                    // Login
                    const loginResult = await client.login('user@example.com', 'password');
                    
                    // Use authenticated client
                    const profile = await client.getProfile();
                    """,
                "mfa_example", """
                    // Handle MFA challenge
                    try {
                      const loginResult = await client.login('user@example.com', 'password');
                    } catch (error) {
                      if (error.code === 'MFA_REQUIRED') {
                        const mfaResult = await client.verifyMfa(error.mfaToken, '123456');
                      }
                    }
                    """
            ),
            "python", Map.of(
                "installation", "pip install crmplatform-auth-sdk",
                "basic_usage", """
                    from crmplatform_auth import AuthClient
                    
                    client = AuthClient(
                        base_url='https://api.crmplatform.com',
                        tenant_id='your-tenant-id'
                    )
                    
                    # Login
                    login_result = client.login('user@example.com', 'password')
                    
                    # Use authenticated client
                    profile = client.get_profile()
                    """,
                "error_handling", """
                    from crmplatform_auth.exceptions import AuthenticationError, MfaRequiredError
                    
                    try:
                        client.login('user@example.com', 'password')
                    except MfaRequiredError as e:
                        client.verify_mfa(e.mfa_token, '123456')
                    except AuthenticationError as e:
                        print(f"Authentication failed: {e.message}")
                    """
            ),
            "java", Map.of(
                "dependency", """
                    <dependency>
                        <groupId>com.crmplatform</groupId>
                        <artifactId>auth-sdk</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                    """,
                "basic_usage", """
                    import com.crmplatform.auth.AuthClient;
                    import com.crmplatform.auth.model.LoginResult;
                    
                    AuthClient client = AuthClient.builder()
                        .baseUrl("https://api.crmplatform.com")
                        .tenantId("your-tenant-id")
                        .build();
                    
                    // Login
                    LoginResult result = client.login("user@example.com", "password");
                    
                    // Use authenticated client
                    UserProfile profile = client.getProfile();
                    """
            )
        );

        return ResponseEntity.ok(ApiResponse.success(examples));
    }
}